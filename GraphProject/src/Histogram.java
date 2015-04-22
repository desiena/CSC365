import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by home on 4/20/15.
 */

class Histogram implements Iterable<HistNode>{
    private static final Set<String> junk = new HashSet<String>(Arrays.asList(
            new String[]{"the", "and", "nbsp", "with", "was", "were", "for", "are", "also", "from"}
    ));
    String url;
    String title;
    int capacity, uniqueCount, totalCount;
    HistNode[] wordFreqs;
    public Histogram(String url) throws Exception{
        this.capacity = 512;
        this.uniqueCount = 0;
        this.totalCount = 0;
        wordFreqs = new HistNode[capacity];
        this.url = url;
        URL myURL = new URL(url);
        BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
        String inputLine, doc = "";
        while ((inputLine = in.readLine()) != null) doc += inputLine + " ";
        in.close();
        Pattern p = Pattern.compile("<title>(.*?)</title>");
        Matcher m = p.matcher(doc);
        while (m.find() == true) {
            this.title=m.group(1);
        }
        doc = Jsoup.clean(doc, Whitelist.none());//remove html tags
        doc = doc.replaceAll("[^a-zA-Z ]", " ").toLowerCase();//remove punctuation, to lower case.
        Scanner sc = new Scanner(doc);
        while(sc.hasNext()) {
            add(sc.next());
        }
    }
    private void checkLoad()throws Exception{
        if(uniqueCount > (capacity*3/4)){
            this.rehash();
        }
    }

    public HistIter iterator(){
        return new HistIter(this);
    }

    private void rehash() throws Exception{
        HistNode[] oldFreqs = this.wordFreqs;
        this.capacity = this.capacity * 2;
        this.wordFreqs = new HistNode[capacity];
        for(HistNode node : oldFreqs){
            while(node!=null){
                this.add(node.word, node.count);
                node=node.next;
            }
        }
    }
    private void add(String word) throws Exception{
        if(word.length() < 3 || junk.contains(word)) return;
        this.checkLoad();
        int hash = (word.hashCode()&capacity-1);
        HistNode node;
        if(wordFreqs[hash] == null) {
            wordFreqs[hash] = new HistNode(word);
            this.uniqueCount++;
            this.totalCount++;
            return;
        }
        node = wordFreqs[hash];
        while(node.next != null || node.word.equals(word)){
            if(node.word.equals(word)){
                node.inc();
                this.totalCount++;
                return;
            }
            node = node.next;
        }
        node.next = new HistNode(word);
        this.uniqueCount++;
        this.totalCount++;
    }
    private void add(String word, int count) throws Exception{
        int hash = (word.hashCode()&capacity-1);
        HistNode node = wordFreqs[hash];
        if(node == null) {
            wordFreqs[hash] = new HistNode(word, count);
            return;
        }
        while(node.next!=null){
            node = node.next;
        }
        node.next = new HistNode(word, count);
    }
    public void display(){
        System.out.println(title + " - Unique: " + uniqueCount + ", Total: " + totalCount +", Capacity: " + capacity);
        /*for(HistNode word : wordFreqs){
            if(word != null) {
                word.display();
            }
        }*/
    }
    public int getCount(String key){
        int hash = (key.hashCode()&capacity-1);
        HistNode node = wordFreqs[hash];
        while (node != null){
            if(node.word.equals(key)){
                return node.count;
            }
            node = node.next;
        }
        return 0;
    }
    public double compare(String doc) throws Exception{
        String word;
        double similarityScore = 0;
        Scanner sc = new Scanner(doc);
        while(sc.hasNext()) {
            word = sc.next();
            similarityScore += getCount(word);
        }
        return similarityScore / totalCount ;
    }
}

class HistIter implements Iterator<HistNode> {
    int bucketIndex;
    Histogram hist;
    HistNode word;
    public HistIter(Histogram hist){
        this.hist = hist;
        bucketIndex = -1;
        while (word == null && bucketIndex < hist.uniqueCount){
            word = hist.wordFreqs[++bucketIndex];
        }
    }
    public boolean hasNext(){
        return word != null;
    }

    public HistNode next(){
        HistNode next = word;
        if(word.next != null){
            word = word.next;
            return next;
        }else if(bucketIndex < hist.capacity - 1) {
            do {
                word = hist.wordFreqs[++bucketIndex];
            }while(word == null && bucketIndex < hist.capacity - 1);
            return next;
        }
        word = null;
        return next;
    }
}

class HistNode{
    String word;
    int count;
    public void inc(){
        count++;
    }
    public HistNode(String word){
        this.word = word;
        this.count = 1;
    }
    public HistNode(String word, int count){
        this.word = word;
        this.count = count;
    }
    public void display(){
        if(this.count > 10 ) {
            System.out.print(word + " " + count + ", ");
        }
        if (next != null) {
            next.display();
        }
    }
    HistNode next;
}