import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

import javax.xml.bind.SchemaOutputResolver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by home on 4/8/15.
 */
public class Crawler {
    ArrayDeque<String> q = new ArrayDeque<String>(512);
    AdjacencyList aj = new AdjacencyList();
    public static String prefix = "http://en.wikipedia.org/wiki/";

    /*
    The purpose fo this crawler is to do a breadth first gathering of wikipedia content pages that are connected by links.
    It does this in a breadth first manner until the receiving adjacency list fills up to its capacity.
     */
    public AdjacencyList breadthFirstCrawl(String startUrl){
        q.add(startUrl);
        while(!q.isEmpty()){
            String url = q.poll();
            try{
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String newUrl = link.attr("abs:href");
                    if (!newUrl.equals("http://en.wikipedia.org/wiki/Main_Page") &&
                            !newUrl.contains(url) &&
                            !newUrl.contains("#")&&
                            newUrl.length() > prefix.length() &&
                            newUrl.substring(0, prefix.length()).equals(prefix)&&
                            !newUrl.substring(prefix.length()).contains("%")&&
                            !newUrl.substring(prefix.length()).contains(":")
                            ){
                        String newTitle = Jsoup.connect(newUrl).get().title();
                        if(!aj.isFull()) {
                            if (aj.add(url, title, newUrl, newTitle)) {
                                q.add(newUrl);
                            }
                        }else{
                            if(aj.contains(newUrl)){
                                aj.add(url, title, newUrl, newTitle);
                            }
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return aj;
    }
}
