package at.jku.isse.ecco;

import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.artifact.ArtifactReference;
import at.jku.isse.ecco.core.Association;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureVersion;
import at.jku.isse.ecco.sg.SequenceGraph;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Trees;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class EccoUtil {

	private EccoUtil() {
	}


	public static Collection<Feature> deepCopyFeatures(Collection<? extends Feature> features, EntityFactory entityFactory) {
		Collection<Feature> copiedFeatures = new ArrayList<>();
		for (Feature feature : features) {
			Feature copiedFeature = entityFactory.createFeature(feature.getId(), feature.getName(), feature.getDescription());

			for (FeatureVersion featureVersion : feature.getVersions()) {
				FeatureVersion copiedFeatureVersion = copiedFeature.addVersion(featureVersion.getId());
				copiedFeatureVersion.setDescription(featureVersion.getDescription());
			}

			copiedFeatures.add(copiedFeature);
		}

		return copiedFeatures;
	}


	/**
	 * Trims all sequence graphs in the given set of associations by removing all artifacts that are not part of the given associations.
	 * Note:
	 * Should being part of an association in this case means solid as well as not solid?
	 * While it should not happen that an artifact is not contained in any of the associations solid (because that would violate dependencies) it could theoretically happen.
	 *
	 * @param associations Associations that contain artifacts to retain in the sequence graphs.
	 */
	public static void trimSequenceGraph(Collection<? extends Association.Op> associations) {
		for (Association.Op association : associations) {
			EccoUtil.trimSequenceGraphRec(associations, association.getRootNode());
		}
	}

	private static void trimSequenceGraphRec(Collection<? extends Association.Op> associations, Node.Op node) {

		if (node.isUnique() && node.getArtifact() != null && node.getArtifact().getSequenceGraph() != null) {
			// get all symbols from sequence graph
			Collection<? extends Artifact.Op<?>> symbols = node.getArtifact().getSequenceGraph().getSymbols();

			// remove symbols that are not contained in the given associations
			Iterator<? extends Artifact.Op<?>> symbolsIterator = symbols.iterator();
			while (symbolsIterator.hasNext()) {
				Artifact<?> symbol = symbolsIterator.next();
				if (!associations.contains(symbol.getContainingNode().getContainingAssociation())) {
					symbolsIterator.remove();
				}
			}

			// trim sequence graph
			node.getArtifact().getSequenceGraph().trim(symbols);
		}

		for (Node.Op child : node.getChildren()) {
			EccoUtil.trimSequenceGraphRec(associations, child);
		}
	}


	/**
	 * Creates a deep copy of a tree using the given entity factory.
	 *
	 * @param node
	 * @param entityFactory
	 * @return
	 */
	public static Node.Op deepCopyTree(Node.Op node, EntityFactory entityFactory) {
		Node.Op node2 = EccoUtil.deepCopyTreeRec(node, entityFactory);

		Trees.updateArtifactReferences(node2);

		return node2;
	}

	private static Node.Op deepCopyTreeRec(Node.Op node, EntityFactory entityFactory) {
		Node.Op node2 = entityFactory.createNode();

		node2.setUnique(node.isUnique());

		if (node.getArtifact() != null) {
			Artifact.Op<?> artifact = node.getArtifact();
			Artifact.Op<?> artifact2;

			boolean firstMatch = false;
			if (artifact.hasReplacingArtifact()) {
				artifact2 = artifact.getReplacingArtifact();
			} else {
				artifact2 = entityFactory.createArtifact(artifact.getData());
				artifact.setReplacingArtifact(artifact2);
				firstMatch = true;
			}

			node2.setArtifact(artifact2);

			if (node.isUnique()) {
				artifact2.setContainingNode(node2);
			}

			artifact2.setAtomic(artifact.isAtomic());
			artifact2.setOrdered(artifact.isOrdered());
			artifact2.setSequenceNumber(artifact.getSequenceNumber());

			// sequence graph
			if (artifact.getSequenceGraph() != null && firstMatch) {
				SequenceGraph.Op sequenceGraph = artifact.getSequenceGraph();
				SequenceGraph.Op sequenceGraph2 = artifact2.createSequenceGraph();

				artifact2.setSequenceGraph(sequenceGraph2);

				// copy sequence graph
				sequenceGraph2.copy(sequenceGraph);
				//sequenceGraph2.sequence(sequenceGraph);
			}

			// TODO: make source and target artifacts both use the same artifact reference instance?
			// references
			// if the target has already been replaced set the uses artifact reference. if not, wait until the target is being processed and set it there as a usedBy. this way no reference is processed twice either. and if the target is never processed, then there is no inconsistent reference.
			if (firstMatch) {
				for (ArtifactReference.Op artifactReference : artifact.getUses()) {
//				ArtifactReference artifactReference2 = entityFactory.createArtifactReference(artifact2, artifactReference.getTarget(), artifactReference.getType());
//				artifact2.addUses(artifactReference2);

					if (artifactReference.getTarget().hasReplacingArtifact())
						artifact2.addUses(artifactReference.getTarget().getReplacingArtifact(), artifactReference.getType());
				}
				for (ArtifactReference.Op artifactReference : artifact.getUsedBy()) {
//				ArtifactReference artifactReference2 = entityFactory.createArtifactReference(artifactReference.getSource(), artifact2, artifactReference.getType());
//				artifact2.addUsedBy(artifactReference2);

					if (artifactReference.getSource().hasReplacingArtifact())
						artifactReference.getSource().getReplacingArtifact().addUses(artifact2, artifactReference.getType());
				}
			}

		} else {
			node2.setArtifact(null);
		}

		for (Node.Op childNode : node.getChildren()) {
			Node.Op childNode2 = EccoUtil.deepCopyTreeRec(childNode, entityFactory);
			node2.addChild(childNode2);
			//childNode2.setParent(node2); // not necessary
		}

		return node2;
	}


	public static String getSHA(Path path) {
		try {
			MessageDigest complete = MessageDigest.getInstance("SHA1");

			try (InputStream fis = Files.newInputStream(path)) {
				byte[] buffer = new byte[1024];
				int numRead = 0;
				while (numRead != -1) {
					numRead = fis.read(buffer);
					if (numRead > 0) {
						complete.update(buffer, 0, numRead);
					}
				}
			}

			return new HexBinaryAdapter().marshal(complete.digest());
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new EccoException("Could not compute hash for " + path, e);
		}
	}

}
