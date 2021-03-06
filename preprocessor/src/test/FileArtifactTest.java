package test;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;

import at.jku.isse.ecco.adapter.dispatch.PluginArtifactData;
import at.jku.isse.ecco.adapter.text.LineArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.exceptions.WrongArtifactDataTypeException;
import at.jku.isse.ecco.exporter.TraceExporter;
import at.jku.isse.ecco.storage.mem.artifact.BaseArtifact;
import at.jku.isse.ecco.storage.mem.core.BaseAssociation;
import at.jku.isse.ecco.storage.mem.module.BasePresenceCondition;
import at.jku.isse.ecco.storage.mem.tree.BaseNode;
import at.jku.isse.ecco.storage.mem.tree.BaseRootNode;
import at.jku.isse.ecco.tree.RootNode;

public class FileArtifactTest {
	RootNode.Op root;
	at.jku.isse.ecco.tree.Node.Op node;
	at.jku.isse.ecco.tree.Node.Op node2;
	at.jku.isse.ecco.tree.Node.Op node3;
	at.jku.isse.ecco.tree.Node.Op node4;
	Artifact.Op<PluginArtifactData> a1 = new BaseArtifact<>(new PluginArtifactData("1", Paths.get("../../Rep/t1.txt")));
	Artifact.Op<LineArtifactData> a2 = new BaseArtifact<>(new LineArtifactData("line1"));
	BasePresenceCondition condition;
	BaseAssociation a;
	
	@Before
	public void init() {
		root = new BaseRootNode();
		node = new BaseNode();
		node2 = new BaseNode();
		node3 = new BaseNode();
		node4 = new BaseNode();
		a1 = new BaseArtifact<>(new PluginArtifactData("1", Paths.get("../../Rep/t1.txt")));
		a2 = new BaseArtifact<>(new LineArtifactData("line1"));
		condition = new BasePresenceCondition();
		a = new BaseAssociation();
	}	
	
	@Test
	public void test() throws WrongArtifactDataTypeException {
		node2.setArtifact(a1);
		node3.setArtifact(a2);
		node.addChildren(node2, node3);
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		TraceExporter te = new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullAssociation() throws WrongArtifactDataTypeException {		
		new TraceExporter(null, Paths.get("../../RepCopy/"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullPath() throws WrongArtifactDataTypeException {
		new TraceExporter(new BaseAssociation(), null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEmptyAssociation() throws WrongArtifactDataTypeException {
		new TraceExporter(new BaseAssociation(), Paths.get("../../Rep/t1.txt"));
	}
	
	@Test
	public void testEmptyNodeL1() throws WrongArtifactDataTypeException { //FIXME root ohne kind sollte ohne Fehler m�glich sein
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test
	public void testEmptyNodeL2() throws WrongArtifactDataTypeException {
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test
	public void testEmptyNodeL3() throws WrongArtifactDataTypeException { //FIXME sollte spezial execption werfen
		node.addChildren(node2);
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test
	public void testEmptyNodeL4() throws WrongArtifactDataTypeException { //FIXME sollte spezial execption werfen
		node2.setArtifact(a1);
		node.addChildren(node2);
		node2.addChildren(node3);
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test(expected=WrongArtifactDataTypeException.class)
	public void testWrongTypeLevel3() throws WrongArtifactDataTypeException {
		node2.setArtifact(a1);
		node3.setArtifact(a2);
		node.addChildren(node2, node3);
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	@Test(expected=WrongArtifactDataTypeException.class)
	public void testWrongTypeLevel4() throws WrongArtifactDataTypeException {
		node2.setArtifact(a1);
		node3.setArtifact(a2);
		node4.setArtifact(a1);
		node.addChildren(node2);
		node2.addChildren(node3, node4);
		root.addChild(node);
		a.setRootNode(root);
		a.setPresenceCondition(condition);
		new TraceExporter(a, Paths.get("../../RepCopy/"));
	}
	
	

}
