/**
 * Created by home on 4/18/15.
 */
import java.util.HashMap;

public class AdjacencyList {
    int pop = 0;
    public static int capacity = 500;
    HashMap<String, Vertex> vertices = new HashMap<String, Vertex>(1024);
    public boolean isFull(){
        return pop >= capacity;
    }

    /*
    Adds any new vertices to the vertex[]. Then once both vertices exist, adds each vertex to the others edge list.
     */
    public boolean add(String url, String title, String newUrl, String newTitle){
        boolean toQ = false;
        Vertex v1, v2;
        if(vertices.containsKey(title)){
            v1 = vertices.get(title);
        }else{
            vertices.put(title, v1 = new Vertex(title, url));
            pop++;
            System.out.println(pop);
            //System.out.println(pop);
        }
        if(vertices.containsKey(newTitle)){
            v2 = vertices.get(newTitle);
        }else{
            vertices.put(newTitle, v2 = new Vertex(newTitle, newUrl));
            toQ = true;
            pop++;
            System.out.println(pop);
            //System.out.println(pop);
        }
        double weight = Vertex.compare(v1, v2);
        v1.addEdge(v2, weight);
        v2.addEdge(v1, weight);
        return toQ;
    }

    public void display(){
        for(Vertex vert: vertices.values()){
            vert.display();
            System.out.println();
        }
    }

    public boolean contains(String url){
        return vertices.containsKey(url);
    }

    public void loadWords(){
        for(Vertex vert: vertices.values()){
            vert.loadWords();
        }
    }
}
