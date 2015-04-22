import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by home on 4/8/15.
 */
public class Vertex {
    String url, title;
    Edge edge;
    HashMap<String, Integer> freqs = new HashMap<String, Integer>();
    public Vertex(String title, String url){
        this.title = title;
        this.url = url;
        loadWords();
    }


    /*
    Adds an edge to the edge list of the vertex, or does nothing if that edge already exists
     */
    public void addEdge(Vertex v, double weight) {
        //System.out.println(v.url);
        if (edge == null) {
            edge = new Edge(v, weight);
        } else if (edge.destination == v) {
            return;
        } else {
            Edge currentEdge = edge;
            while (currentEdge.next != null) {
                if (currentEdge.next.destination == v) {
                    return;
                }
                currentEdge = currentEdge.next;
            }
            currentEdge.next = new Edge(v, weight);
        }
    }

    public static double compare(Vertex v1, Vertex v2){
        int dotProduct = 0, sumOfSquaresV1 = 0,  sumOfSquaresV2 = 0, v1freq = 0, v2freq = 0;
        HashMap<String, Integer> lex = (HashMap<String, Integer>)v1.freqs.clone();
        lex.putAll(v2.freqs);
        Set<String> lexicon = lex.keySet();
        for(String word : lexicon){
            v1freq = v1.freqs.containsKey(word) ? v1.freqs.get(word): 0;
            v2freq = v2.freqs.containsKey(word) ? v2.freqs.get(word): 0;
            dotProduct += v1freq * v2freq;
            sumOfSquaresV1 += Math.pow(v1freq, 2);
            sumOfSquaresV2 += Math.pow(v2freq, 2);
        }
        return dotProduct/(Math.sqrt(sumOfSquaresV1)*Math.sqrt(sumOfSquaresV2));
    }

    public void loadWords(){
        try {
            URL myURL = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
            String inputLine, doc = "";
            while ((inputLine = in.readLine()) != null) doc += inputLine + " ";
            in.close();
            doc = Jsoup.clean(doc, Whitelist.none());//remove html tags
            doc = doc.replaceAll("[^a-zA-Z ]", " ").toLowerCase();//remove punctuation, to lower case.
            Scanner sc = new Scanner(doc);
            String word;
            while (sc.hasNext()) {
                word = sc.next();
                if (word.length() > 3) {
                    addWord(word);
                }
            }
        }catch(MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void addWord(String word){
        int count = freqs.containsKey(word) ? freqs.get(word) : 0;
        freqs.put(word, count + 1);
    }

    public void display(){
        int prefixLength = Crawler.prefix.length();
        System.out.println(url.substring(prefixLength) + " connects to:");
        Edge ed = edge;
        while(ed != null){
            System.out.println(ed.destination.url.substring(prefixLength) + ed.weight);
            ed = ed.next;
        }
    }
}
