import javax.net.ssl.SSLEngineResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Router {

    private HashMap<Router, Integer> distances;
    private String name;
    public Router(String name) {
        this.distances = new HashMap<>();
        this.name = name;
    }

    public void onInit() throws InterruptedException {

        //TODO: IMPLEMENT ME
        //As soon as the network is online,
        //fill in your initial distance table and broadcast it to your neighbors
        distances = new HashMap<>();
        for (Neighbor neigh: Network.getNeighbors(this)) {
            distances.put(neigh.router, neigh.cost);
        }
        for (Neighbor neigh: Network.getNeighbors(this)) {
            Message distanceVector = new Message(this, neigh.router, distances);
            Network.sendDistanceMessage(distanceVector);
        }
    }

    public void onDistanceMessage(Message message) throws InterruptedException {
        //update your distance table and broadcast it to your neighbors if it changed
        HashSet<Neighbor> neighbors = Network.getNeighbors(this);
        boolean tableUpdated = false;

        if (message.receiver == this && neighbors != null && neighbors.stream().anyMatch(neighbor -> neighbor.router.equals(message.sender))) {
            int distToNeigh = distances.get(message.sender);
            HashMap<Router, Integer> neighDistances = message.distances;
            Set<Router> keySet = neighDistances.keySet();
            for (Router router: keySet) {
                int shortestDist = distToNeigh + neighDistances.get(router);
                if (distances.containsKey(router)) {
                    if (distances.get(router) > shortestDist) {
                        distances.put(router, shortestDist);
                        tableUpdated = true;
                    }
                }
                else {
                    distances.put(router, shortestDist);
                    tableUpdated = true;
                }
            }
            if (tableUpdated) {
                for (Neighbor neigh: Network.getNeighbors(this)) {
                    Message distanceVector = new Message(this, neigh.router, distances);
                    Network.sendDistanceMessage(distanceVector);
                }
            }
        }
    }


    public void dumpDistanceTable() {
        System.out.println("router: " + this);
        for(Router r : distances.keySet()){
            System.out.println("\t" + r + "\t" + distances.get(r));
        }
    }

    @Override
    public String toString(){
        return "Router: " + name;
    }
}