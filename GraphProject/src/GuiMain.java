/**
 * Created by home on 4/8/15.
 */
public class GuiMain {
    public static void main(String[] args){
        AdjacencyList adjl = new Crawler().breadthFirstCrawl("http://en.wikipedia.org/wiki/Kalman_filter");
        adjl.display();
    }
}
