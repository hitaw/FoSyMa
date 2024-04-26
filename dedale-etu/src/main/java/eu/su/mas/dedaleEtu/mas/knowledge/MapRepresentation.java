package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

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
		agent, open, closed, openStench, stench;
    }

	private List<String> plannedItinerary = null;
	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private final String defaultNodeStyle = "node {" + "fill-color: black;"
			+ " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private final String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
	private final String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
	private final String nodeStyle_stench = "node.stench {" + "fill-color: yellow;" + "}";
    private final String nodeStyle_openStench = "node.openStench {" + "fill-color: green;" + "}";

	private final String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open + nodeStyle_stench + nodeStyle_openStench;

	private Graph g; // data structure non serializable
	private Viewer viewer; // ref to the display, non-serializable
	private Integer nbEdges;// used to generate the edges ids

	private SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>> sg;// used as a temporary dataStructure during migration

	public MapRepresentation() {
		// System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		Platform.runLater(this::openGui);
		// this.viewer = this.g.display();

		this.nbEdges = 0;
	}
	public MapRepresentation(boolean display) {
		// System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		if (display) {
			Platform.runLater(this::openGui);
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
		int stench = 0;
		if (this.g.getNode(id) == null) {
			n = this.g.addNode(id);
		} else {
			n = this.g.getNode(id);
			stench = getStenchValue(n); // The stench value is pondered by date
		}
		n.clearAttributes();
		if (stench>0) {
			if (mapAttribute == MapAttribute.open) {
				mapAttribute = MapAttribute.openStench;
			} else if (mapAttribute == MapAttribute.closed) {
				mapAttribute = MapAttribute.stench;
			}
		}

		n.setAttribute("ui.class", mapAttribute.toString());
		n.setAttribute("ui.label", id);
		n.setAttribute("stench.date", new Date());
		n.setAttribute("stench.count", stench);
	}

	public synchronized void addNode(String id, MapAttribute mapAttribute, boolean stench) {
		addNode(id, mapAttribute);
		Node n = this.g.getNode(id);
        if (stench) {
			n.setAttribute("stench.count", getStenchValue(n) + 1);
			if (mapAttribute == MapAttribute.open) {
                mapAttribute = MapAttribute.openStench;
            }
            if (mapAttribute == MapAttribute.closed) {
                mapAttribute = MapAttribute.stench;
            }
        } else {
			n.setAttribute("stench.count", 0);
		}
        n.setAttribute("ui.class", mapAttribute.toString());
        n.setAttribute("stench.date", new Date());
	}

	public synchronized void addNode(String id, MapAttribute mapAttribute, Date dateStench, int countStench) {
        if ((countStench > 0) && (mapAttribute == mapAttribute.closed)) {
            mapAttribute = mapAttribute.stench;
        } else if ((countStench > 0) && (mapAttribute == mapAttribute.open)) {
            mapAttribute = mapAttribute.openStench;
        }
		addNode(id, mapAttribute);
		Node n = this.g.getNode(id);
		n.setAttribute("stench.date", dateStench);
		n.setAttribute("stench.count", countStench);
	}

	/**
	 * Add a node to the graph. Updates stench date if the node already exists. If new, it is
	 * labeled as open (non-visited)
	 * 
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id, boolean stench) {
		if (this.g.getNode(id) == null) {
			addNode(id, MapAttribute.open, stench);
			return true;
		}

        // Updates the status to update the color of the node
        Node n = this.g.getNode(id);
        String nodeClass = (String) n.getAttribute("ui.class");
		// Si le noeud n'était pas puant et qu'on dit qu'il l'est, on change sa classe à stench
        if (getStenchValue(n) == 0 && stench) {
            if (nodeClass == MapAttribute.open.toString()) {
                n.setAttribute("ui.class", MapAttribute.openStench.toString());
            } else if (nodeClass == MapAttribute.closed.toString()) {
                n.setAttribute("ui.class", MapAttribute.stench.toString());
            }
        } else if (!stench) { // S'il n'y a pas d'odeur on retire la couleur d'odeur et le compte
			n.setAttribute("stench.count", 0);
            if (nodeClass == MapAttribute.openStench.toString()) {
                n.setAttribute("ui.class", MapAttribute.open.toString());
            } else if (nodeClass == MapAttribute.stench.toString()) {
                n.setAttribute("ui.class", MapAttribute.closed.toString());
            }
        }

		if (stench) n.setAttribute("stench.count", getStenchValue(n) + 1);
		n.setAttribute("stench.date", new Date()); // update the stench date on an already existing node
		return false;
	}

	public int getStenchValue(Node n) {
		int count = (Integer) n.getAttribute("stench.count");
		Date date = (Date) n.getAttribute("stench.date");

		Date now = new Date();
		long diff = now.getTime() - date.getTime();
		long diffIt = diff / (WaitTime*30);
		// We consider that the stench is reduced by one every WaitTime*30
		// This is completely arbitrary and might need adjustment for better performance
		return (int) Math.max(0, count - diffIt);

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
			System.err.println("Edge rejected " + agent);
		} catch (ElementNotFoundException e3) {

		}
	}

	public synchronized Edge removeEdge(String from, String to) {
		Edge edge = null;
		try {
			edge = this.g.removeEdge(from, to);
		}
		catch (ElementNotFoundException e){
			System.out.println("Edge not in graph");
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
        if (idFrom == idTo) {
            return shortestPath;
        }

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


	public void setPlannedItinerary(List<String> plannedItinerary) {
		this.plannedItinerary = plannedItinerary;
	}

	public void clearPlannedItinerary() {
		this.plannedItinerary = null;
	}

	public synchronized String getNextNodePlan() {
		if (plannedItinerary == null || plannedItinerary.isEmpty()){
			return null;
		}
		return plannedItinerary.get(0);
	}

    public String getLastNodePlan() {
        if (plannedItinerary == null || plannedItinerary.isEmpty()){
            return null;
        }
        return plannedItinerary.get(plannedItinerary.size() - 1);
    }
	public boolean destinationInRange(int talkingRange) {
		return ((plannedItinerary != null) && (plannedItinerary.size() <= talkingRange));
	}

	public synchronized void advancePlan() {
		if (plannedItinerary != null && !plannedItinerary.isEmpty()) {
			plannedItinerary.remove(0);
		}
		else {
			plannedItinerary = null;
		}
	}

	public List<String> getOpenNodes() {
		return this.g.nodes().filter(x -> (x.getAttribute("ui.class") == MapAttribute.open.toString() || x.getAttribute("ui.class") == MapAttribute.openStench.toString())).map(Node::getId)
				.collect(Collectors.toList());
	}

	/**
	 * Before the migration we kill all non-serializable components and store their
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
		this.sg = new SerializableSimpleGraph<String, Couple<MapAttribute,Couple<Date, Integer>>>();
		Iterator<Node> iter = this.g.iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			sg.addNode(n.getId(), new Couple(MapAttribute.valueOf((String) n.getAttribute("ui.class")), new Couple(n.getAttribute("stench.date"), n.getAttribute("stench.count"))));
		}
		Iterator<Edge> iterE = this.g.edges().iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
	}

	public synchronized SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>> getSerializableGraph() {
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration, we load the serialized data and recreate the non-serializable
	 * components (Gui,...)
	 */
	public synchronized void loadSavedData() {

		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		openGui();

		Integer nbEd = 0;
		for (SerializableNode<String, Couple<MapAttribute, Couple<Date, Integer>>> n : this.sg.getAllNodes()) {
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().getLeft().toString());
			this.g.getNode(n.getNodeId()).setAttribute("stench.date", n.getNodeContent().getRight().getLeft());
			this.g.getNode(n.getNodeId()).setAttribute("stench.count", n.getNodeContent().getRight().getRight());
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

	public void mergeMap(SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>> sgreceived) {

		for (SerializableNode<String, Couple<MapAttribute, Couple<Date, Integer>>> n : sgreceived.getAllNodes()) {
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
				newnode.setAttribute("stench.date", n.getNodeContent().getRight().getLeft());
				newnode.setAttribute("stench.count", n.getNodeContent().getRight().getRight());
			} else {
				newnode = this.g.getNode(n.getNodeId());
				// 3 check its attribute. If it is below the one received, update it.
                Date nDate = n.getNodeContent().getRight().getLeft();
                if (((Date) newnode.getAttribute("stench.date")).before(nDate)) {
                    newnode.setAttribute("stench.date", nDate);
                    newnode.setAttribute("stench.count", n.getNodeContent().getRight().getRight());
                }
                // If the node was closed in the new version
                if (n.getNodeContent().getLeft().toString() == MapAttribute.closed.toString()) {
                    newnode.setAttribute("ui.class", MapAttribute.closed.toString());
                } else if (n.getNodeContent().getLeft().toString() == MapAttribute.stench.toString()) {
                    newnode.setAttribute("ui.class", MapAttribute.stench.toString());
                }
                if ((getStenchValue(newnode) != 0) && (newnode.getAttribute("ui.class").toString() == MapAttribute.open.toString())) {
                    newnode.setAttribute("ui.class", MapAttribute.openStench.toString());
                } else if ((getStenchValue(newnode) == 0) && (newnode.getAttribute("ui.class").toString() == MapAttribute.openStench.toString())) {
                    newnode.setAttribute("ui.class", MapAttribute.open.toString());
                }
			}
		}

		// 4 now that all nodes are added, we can add edges
		for (SerializableNode<String, Couple<MapAttribute, Couple<Date, Integer>>> n : sgreceived.getAllNodes()) {
			for (String s : sgreceived.getEdges(n.getNodeId())) {
				addEdge(n.getNodeId(), s);
			}
		}
		this.clearPlannedItinerary(); // The map changed, we want to recalculate the itinerary
		// System.out.println("Merge done");
	}

	/**
	 * If the given map has additional nodes compared to *this* it won't be taken. This only searches for elements that were added on *this*. It is meant to be used to update m
	 * For example : if it worked on lists : [a,b,c].getDiff([a,b,d]) would return [c]
	 * @param m The smaller map to compare with this
	 * @return the nodes and edges that were added to the map
	 */
	public SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>> getDiff(MapRepresentation m) {
		MapRepresentation diff = new MapRepresentation(false);
		// If a node from this is not in m, add it to diff
		for (Node n : this.g) {
			MapAttribute attr = MapAttribute.valueOf((String) n.getAttribute("ui.class"));
			Date stenchDate = (Date) n.getAttribute("stench.date");
			int stenchCount = (Integer) n.getAttribute("stench.count");

			if (m.g.getNode(n.getId()) == null) {
				diff.addNode(n.getId(), attr, stenchDate, stenchCount);
			} else { // if a node changed status from open to closed, add it to diff
				String nAttribute = attr.toString();
				String mAttribute = m.g.getNode(n.getId()).getAttribute("ui.class").toString();

				// Since this function is meant to compare a map with a new version of it, if there is a status change, we keep the version stored in *this*
				if (!Objects.equals(nAttribute, mAttribute)) {
					diff.addNode(n.getId(), attr, stenchDate, stenchCount);
				}
				// If the stench count changed, we add it too
				if ((Integer) m.g.getNode(n.getId()).getAttribute("stench.count") != stenchCount) {
					diff.addNode(n.getId(), attr, stenchDate, stenchCount);
				}
			}
		}
		if (diff.g.getNodeCount() == 0) {
			return null;
		}
		// Add every edge known on modified nodes
		for (Node n : diff.g) {
			for (String s : this.getSerializableGraph().getEdges(n.getId())) {
				if (diff.g.getNode(s) == null) { // if the node is not in diff, add it, otherwise it will try to add an edge to a non-existing node
					MapAttribute attr = MapAttribute.valueOf((String) this.g.getNode(s).getAttribute("ui.class"));
					Date stenchDate = (Date) this.g.getNode(s).getAttribute("stench.date");
					int stenchCount = getStenchValue(this.g.getNode(s));

					diff.addNode(s, attr, stenchDate, stenchCount);
				}
				diff.addEdge(n.getId(), s);
			}
		}
		return diff.getSerializableGraph();
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph
	 */
	public boolean hasOpenNode() { // TODO noeuds à voir plus tard
		return (this.g.nodes().filter(n -> ((n.getAttribute("ui.class") == MapAttribute.open.toString()) || (n.getAttribute("ui.class") == MapAttribute.openStench.toString()))).findAny())
				.isPresent();
	}

	/**
	 * This function returns the node that has the most smell, considering that smell fades over time
	 * @return the node that has the most pertinent smell to explore
	 */
	public String getStinkiestNode() {
		Node res = this.g.getNode(0);
		for (Node n : this.g) {
			if (getStenchValue(n) > getStenchValue(res)) {
				res = n;
			}
		}
		if (getStenchValue(res) == 0) {
			return null;
		}
		return res.getId();
	}

    public List<String> getNodesArityMax(int maxArity) {
        List<String> res = new ArrayList<>();
        for (Node n : this.g) {
            if (n.getDegree() <= maxArity) {
                res.add(n.getId());
            }
        }
        return res;
    }

	private int heuristic(int degre, int distance) {
		return degre * (distance+1);
	}

    public Map<String, Integer> getCloseNodesMaxArity(int maxArity, int maxDistance, String posId) {
        List<String> possibleNodes = getNodesArityMax(maxArity);
        Map<String, Integer> res = new HashMap<String, Integer>();
        List<String> shortestPath;
        for (String node : possibleNodes) {
            shortestPath = getShortestPath(posId, node);
            if ((shortestPath!=null) && (shortestPath.size() <= maxDistance)) {
                res.put(node, heuristic(this.g.getNode(node).getDegree(), shortestPath.size()));
            }
        }
        return res;
    }

	/**
	 * Get the neighbors of a node
	 * @param node the node to get the neighbors
	 * @return a list of the neighbors
	 * **/
	private List<String> neighborNodes(String node) {
		Node n = this.g.getNode(node);
		Stream<Node> nodes = n.neighborNodes();
		return nodes.map(Node::getId).collect(Collectors.toList());
	}

	/**
	 * Get the neighbors of the neighbors of a node
	 * @param node the node to get the neighbors of the neighbors
	 * @return a list of the neighbors of the neighbors
	 */
	private List<String> neighborNodes2(String node) {
		Node n = this.g.getNode(node);
		Set<String> res = new HashSet<>();
		Stream<Node> nodes = n.neighborNodes();
		Stream<Stream<Node>> furtherNodes = nodes.map(Node::neighborNodes);
		furtherNodes.forEach(s -> s.forEach(n2 -> res.add(n2.getId())));
        res.remove(node);
		return new ArrayList<>(res);
	}

	/**
	 * gets the nodes to form a line of hunt
	 * @param node the node with a golem
	 * @param dest the node where the golem must be stuck
	 * @return a list of the nodes to form a line of hunt
	 */
	public List<String> neighborLine(String node, String dest) {
		int len = getShortestPath(node, dest).size();
		List<String> neighbors = neighborNodes(node);
		List<String> res = new ArrayList<>();
		for (String n: neighbors) {
			if (getShortestPath(n, dest).size() == len + 1) res.add(n);
		}
		// Also add the nodes at 2 nodes distance
		List<String> neighbors2 = neighborNodes2(node);
		for (String n2 : neighbors2) {
			if (getShortestPath(n2, dest).size() == len)  res.add(n2);
		}
		return res;
	}

    public boolean isNeighbor(String node1, String node2) {
        return g.getNode(node1).neighborNodes().anyMatch(n -> n.getId().equals(node2));
    }

	public String getRandomNode() {
		List<String> nodes = this.g.nodes().map(Node::getId).collect(Collectors.toList());
		return nodes.get(new Random().nextInt(nodes.size()));
	}

}