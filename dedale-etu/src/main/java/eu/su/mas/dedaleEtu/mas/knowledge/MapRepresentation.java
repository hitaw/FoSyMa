package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import javafx.application.Platform;

/**
 * This simple topology representation only deals with the graph, not its
 * content.</br>
 * The knowledge representation is not well written (at all), it is just given
 * as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the
 * dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * 
	 * @author hc
	 *
	 */

	public enum MapAttribute {
		agent, open, closed;

	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle = "node {" + "fill-color: black;"
			+ " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
	private String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
	private String nodeStyle_stench = "node.stench {" + "fill-color: yellow;" + "}";
	private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open + nodeStyle_stench;

	private Graph g; // data structure non serializable
	private Viewer viewer; // ref to the display, non-serializable
	private Integer nbEdges;// used to generate the edges ids

	private SerializableSimpleGraph<String, Couple<MapAttribute, Date>> sg;// used as a temporary dataStructure during migration

	public MapRepresentation() {
		// System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		Platform.runLater(() -> {
			openGui();
		});
		// this.viewer = this.g.display();

		this.nbEdges = 0;
	}
	public MapRepresentation(boolean display) {
		// System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		if (display) {
			Platform.runLater(() -> {
				openGui();
			});
		}
		// this.viewer = this.g.display();

		this.nbEdges = 0;
	}

	/**
	 * Add or replace a node and its attribute
	 * 
	 * @param id           unique identifier of the node
	 * @param mapAttribute attribute to process
	 */
	public synchronized void addNode(String id, MapAttribute mapAttribute) {
		Node n;
		Date stench = null;
		if (this.g.getNode(id) == null) {
			n = this.g.addNode(id);
		} else {
			n = this.g.getNode(id);
			stench = (Date) n.getAttribute("stench.date");
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.toString());
		n.setAttribute("ui.label", id);
		n.setAttribute("stench.date", stench);
	}

	public synchronized void addNode(String id, MapAttribute mapAttribute, Date stenchDate) {
		addNode(id, mapAttribute);
		Node n = this.g.getNode(id);
		n.setAttribute("stench.date", stenchDate);
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists. If new, it is
	 * labeled as open (non-visited)
	 * 
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id, Date stench) {
		if (this.g.getNode(id) == null) {
			addNode(id, MapAttribute.open, stench);
			return true;
		}
		return false;
	}

	/**
	 * Add an undirect edge if not already existing.
	 * 
	 * @param idNode1 unique identifier of node1
	 * @param idNode2 unique identifier of node2
	 */
	public synchronized void addEdge(String idNode1, String idNode2) {
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		} catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		} catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch (ElementNotFoundException e3) {

		}
	}

	public synchronized void addEdge(String idNode1, String idNode2, String idEdge, String agent) {
		try {
			this.g.addEdge(idEdge, idNode1, idNode2);
			System.out.println("Adding edge between " + idNode1 + " and " + idNode2);
		} catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing" + agent);
			System.exit(1);
		} catch (EdgeRejectedException e2) {
			System.err.println("Edge rejected" + agent);
		} catch (ElementNotFoundException e3) {

		}
	}

	public synchronized Edge removeEdge(String from, String to) {
		Edge edge = null;
		try {
			edge = this.g.removeEdge(from, to);
		}
		catch (ElementNotFoundException e){
			System.out.println("Edge not in graph, one node missing");
		}
		return edge;
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently
	 * not very efficient
	 * 
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo   id of the destination node
	 * @return the list of nodes to follow, null if the targeted node is not
	 *         currently reachable
	 */
	public synchronized List<String> getShortestPath(String idFrom, String idTo) {
		List<String> shortestPath = new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();// number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();// compute the distance to all nodes from idFrom
		List<Node> path = dijkstra.getPath(g.getNode(idTo)).getNodePath(); // the shortest path from idFrom to idTo
		Iterator<Node> iter = path.iterator();
		while (iter.hasNext()) {
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if (shortestPath.isEmpty()) {// The openNode is not currently reachable
			return null;
		} else {
			shortestPath.remove(0);// remove the current position
		}
		return shortestPath;
	}

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		// 1) Get all openNodes
		List<String> opennodes = getOpenNodes();

		// 2) select the closest one
		List<Couple<String, Integer>> lc = opennodes.stream()
				.map(on -> (getShortestPath(myPosition, on) != null)
						? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size())
						: new Couple<String, Integer>(on, Integer.MAX_VALUE))// some nodes may be unreachable if the
																				// agents do not share at least one
																				// common node.
				.collect(Collectors.toList());

		Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
		// 3) Compute shorterPath

		return getShortestPath(myPosition, closest.get().getLeft());
	}

	public List<String> getOpenNodes() {
		return this.g.nodes().filter(x -> x.getAttribute("ui.class") == MapAttribute.open.toString()).map(Node::getId)
				.collect(Collectors.toList());
	}

	/**
	 * Before the migration we kill all non serializable components and store their
	 * data in a serializable form
	 */
	public void prepareMigration() {
		serializeGraphTopology();

		closeGui();

		this.g = null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg = new SerializableSimpleGraph<String, Couple<MapAttribute,Date>>();
		Iterator<Node> iter = this.g.iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			sg.addNode(n.getId(), new Couple(MapAttribute.valueOf((String) n.getAttribute("ui.class")), n.getAttribute("stench.date")));
		}
		Iterator<Edge> iterE = this.g.edges().iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
	}

	public synchronized SerializableSimpleGraph<String, Couple<MapAttribute, Date>> getSerializableGraph() {
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non-serializable
	 * components (Gui,..)
	 */
	public synchronized void loadSavedData() {

		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		openGui();

		Integer nbEd = 0;
		for (SerializableNode<String, Couple<MapAttribute, Date>> n : this.sg.getAllNodes()) {
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().getLeft().toString());
			this.g.getNode(n.getNodeId()).setAttribute("stench.date", n.getNodeContent().getRight());
			for (String s : this.sg.getEdges(n.getNodeId())) {
				this.g.addEdge(nbEd.toString(), n.getNodeId(), s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non-serializable graphStream
	 * components
	 */
	private synchronized void closeGui() {
		// once the graph is saved, clear non-serializable components
		if (this.viewer != null) {
			// Platform.runLater(() -> {
			try {
				this.viewer.close();
			} catch (NullPointerException e) {
				System.err.println(
						"Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			// });
			this.viewer = null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer = new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);// GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}

	public void mergeMap(SerializableSimpleGraph<String, Couple<MapAttribute, Date>> sgreceived) {
		// System.out.println("You should decide what you want to save and how");
		// System.out.println("We currently blindly add the topology");

		for (SerializableNode<String, Couple<MapAttribute, Date>> n : sgreceived.getAllNodes()) {
			// System.out.println(n);
			boolean alreadyIn = false;
			// 1 Add the node
			Node newnode = null;
			try {
				newnode = this.g.addNode(n.getNodeId());
			} catch (IdAlreadyInUseException e) {
				alreadyIn = true;
				// System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());
				newnode.setAttribute("ui.class", n.getNodeContent().getLeft().toString());
				newnode.setAttribute("stench.date", n.getNodeContent().getRight());
			} else {
				newnode = this.g.getNode(n.getNodeId());
				// 3 check its attribute. If it is below the one received, update it.
				if (((String) newnode.getAttribute("ui.class")) == MapAttribute.closed.toString()
						|| n.getNodeContent().toString() == MapAttribute.closed.toString()) {
					newnode.setAttribute("ui.class", MapAttribute.closed.toString());
				}
				// if the date of the stench is more recent, update it
				if (n.getNodeContent().getRight() != null) {
					if ((Date) newnode.getAttribute("stench.date") == null
							|| ((Date) newnode.getAttribute("stench.date")).before(n.getNodeContent().getRight())) {
						newnode.setAttribute("stench.date", n.getNodeContent().getRight());
					}
				}
			}
		}

		// 4 now that all nodes are added, we can add edges
		for (SerializableNode<String, Couple<MapAttribute, Date>> n : sgreceived.getAllNodes()) {
			for (String s : sgreceived.getEdges(n.getNodeId())) {
				addEdge(n.getNodeId(), s);
			}
		}
		// System.out.println("Merge done");
	}

	/**
	 * If the given map has additional nodes compared to *this* it won't be taken. This only searches for elements that were added on *this*. It is meant to be used to update m
	 * For example : if it worked on lists : [a,b,c].getDiff([a,b,d]) would return [c]
	 * TODO renvoyer les nouvelles stench aussi
	 * @param m The smaller map to compare with this
	 * @return the nodes and edges that were added to the map
	 */
	public SerializableSimpleGraph<String, Couple<MapAttribute, Date>> getDiff(MapRepresentation m) {
		MapRepresentation diff = new MapRepresentation(false);
		// If a node from this is not in m, add it to diff
		for (Node n : this.g) {
			MapAttribute attr = MapAttribute.valueOf((String) n.getAttribute("ui.class"));
			Date stenchDate = (Date) n.getAttribute("stench.date");
			if (m.g.getNode(n.getId()) == null) {
				diff.addNode(n.getId(), attr, stenchDate);
			} else { // if a node changed status from open to closed, add it to diff
				if (n.getAttribute("ui.class") != m.g.getNode(n.getId()).getAttribute("ui.class")) {
					diff.addNode(n.getId(), MapAttribute.closed, stenchDate);
				}
			}
		}
		// Add every edge known on modified nodes
		for (Node n : diff.g) {
			for (String s : this.getSerializableGraph().getEdges(n.getId())) {
				if (diff.g.getNode(s) == null) { // if the node is not in diff, add it, otherwise it will try to add an edge to a non-existing node
					MapAttribute attr = MapAttribute.valueOf((String) this.g.getNode(s).getAttribute("ui.class"));
					Date stenchDate = (Date) this.g.getNode(s).getAttribute("stench.date");
					diff.addNode(s, attr, stenchDate);
				}
				diff.addEdge(n.getId(), s);
			}
		}
		if (diff.g.getNodeCount() == 0) {
			return null;
		}
		return diff.getSerializableGraph();
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes().filter(n -> n.getAttribute("ui.class") == MapAttribute.open.toString()).findAny())
				.isPresent();
	}

}