package fishnoi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Graph<N> {

    private Map<N, Set<N>> theNeighbors = new HashMap<N, Set<N>>();
    private Set<N> theNodeSet = Collections.unmodifiableSet(theNeighbors.keySet());

    public void add (N node) {
        if (theNeighbors.containsKey(node)) return;
        theNeighbors.put(node, new ArraySet<N>());
     }

    public void add (N nodeA, N nodeB) throws NullPointerException {
        theNeighbors.get(nodeA).add(nodeB);
        theNeighbors.get(nodeB).add(nodeA);
    }

    public void remove (N node) {
        if (!theNeighbors.containsKey(node)) return;
        for (N neighbor: theNeighbors.get(node))
            theNeighbors.get(neighbor).remove(node);    
        theNeighbors.get(node).clear();                 
        theNeighbors.remove(node);                      
    }

    public void remove (N nodeA, N nodeB) throws NullPointerException {
        theNeighbors.get(nodeA).remove(nodeB);
        theNeighbors.get(nodeB).remove(nodeA);
    }

    public Set<N> neighbors (N node) throws NullPointerException {
        return Collections.unmodifiableSet(theNeighbors.get(node));
    }

    public Set<N> nodeSet () {
        return theNodeSet;
    }

}
