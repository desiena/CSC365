/**
 * Created by home on 3/23/15.
 */
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.safety.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class btreeproject {
    public static void main(String[] args) throws Exception {
        Btree btree = new Btree("btree");
        btree.load();
        btree.display();
    }
}

class Btree{
    RandomAccessFile file;
    int nodeCount;
    BtreeNode root;
    public static final short minimumDegree = 500;

    public Btree(String name)throws Exception{
        File file = new File(name);
        if(file.isFile()){
            this.file = new RandomAccessFile(file, "rw");
            root = readNode(this.file.readInt());
            int length = (int)this.file.length();
            nodeCount = (int) ((length - 516) / (168 * minimumDegree - 77));
        }else{
            file.createNewFile();
            this.file = new RandomAccessFile(file, "rw");
            root = new BtreeNode(0);
            root.setLeaf(true);
            writeNode(root);
            nodeCount = 1;
        }
    }

    public void loadWords(String url, int urlIndex) throws Exception{
        Histogram histogram = new Histogram(url);
        //System.out.println(histogram.uniqueCount);
        for(HistNode histNode: histogram){
            if(histNode.count == 0){
                System.out.println(histNode.word);
            }else {
                insert(histNode.word, urlIndex, histNode.count);
            }
        }
    }

    public void loadPage(String url, int urlIndex)throws Exception{
        System.out.println("loading " + url);
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        String newUrl;
        for (Element link : links) {
            newUrl = link.attr("abs:href");
            if(!newUrl.contains(url)){
                try{
                    loadWords(newUrl,urlIndex);
                }catch (FileNotFoundException e){
                    //System.out.println("Bad url: " + newUrl);
                }catch (IOException e){
                    //e.printStackTrace();
                }
            }
        }
    }

    public void load() throws Exception{
        String[] pages =   {"http://en.wikipedia.org/wiki/List_of_sandwiches",
                            "http://en.wikipedia.org/wiki/List_of_articles_about_things_which_are_artificial",
                            "http://en.wikipedia.org/wiki/Newspaper_endorsements_in_the_United_States_presidential_election,_2012",
                            "http://en.wikipedia.org/wiki/List_of_foodborne_illness_outbreaks_in_the_United_States",
                            "http://en.wikipedia.org/wiki/Outline_of_smoking",
                            "http://en.wikipedia.org/wiki/List_of_decorative_stones",
                            "http://en.wikipedia.org/wiki/List_of_pine_barrens",
                            "http://en.wikipedia.org/wiki/List_of_herbaria",
                            "http://en.wikipedia.org/wiki/List_of_prison_escapes",
                            "http://en.wikipedia.org/wiki/List_of_trips_made_by_Dmitry_Medvedev",
                            "http://en.wikipedia.org/wiki/List_of_free-software_events",
                            "http://en.wikipedia.org/wiki/List_of_Renaissance_fairs",
                            "http://en.wikipedia.org/wiki/List_of_knots",
                            "http://en.wikipedia.org/wiki/List_of_philatelic_museums",
                            "http://en.wikipedia.org/wiki/List_of_museums_of_Asian_art",
                            "http://en.wikipedia.org/wiki/List_of_Spider-Man_enemies",
                            "http://en.wikipedia.org/wiki/List_of_comic_book_drugs",
                            "http://en.wikipedia.org/wiki/List_of_Kill_Bill_characters",
                            "http://en.wikipedia.org/wiki/Gardens_of_Alsace",
                            "http://en.wikipedia.org/wiki/List_of_typefaces"
        };
        int i = 0;
        for (String url: pages){
            if(i == 5){
                loadPage(url, i);
            }
            i++;
        }
    }

    public boolean searchAndAdd(BtreeNode node, String key, int url, int freq) throws Exception{
        int i = 0;
        int pop = node.getPopulation();
        while(i < pop && key.compareTo(new String(node.getKey(i))) > 0)i ++;
        if(i < pop) {
            String word = new String(node.getKey(i));
            word = word.trim();
            if(key.equals(word)) {
                node.addFreq(i, url, freq);
                writeNode(node);
                return true;
            }
        }
        if(node.getLeaf()){
            return false;
        }else{
            return searchAndAdd(readNode(node.getChild(i)), key, url, freq);
        }
    }

    public short[] search(String key) throws Exception{
        int i = 0;
        int pop = root.getPopulation();
        while(i < pop && key.compareTo(new String(root.getKey(i))) > 0)i ++;
        if(i < pop) {
            String word = new String(root.getKey(i));
            word = word.trim();
            if(key.equals(word)) {
                return root.getFreqs(i);
            }
        }
        if(root.getLeaf()){
            return null;
        }else{
            return search(readNode(root.getChild(i)), key);
        }
    }

    public short[] search(BtreeNode node, String key) throws Exception{
        int i = 0;
        int pop = node.getPopulation();
        while(i < pop && key.compareTo(new String(node.getKey(i))) > 0)i ++;
        if(i < pop) {
            String word = new String(node.getKey(i));
            word = word.trim();
            if(key.equals(word)) {
                return node.getFreqs(i);
            }
        }
        if(root.getLeaf()){
            return null;
        } else {
            return search(readNode(node.getChild(i)), key);
        }
    }

    public BtreeNode splitChild(BtreeNode parent, BtreeNode oldChild, int index) throws Exception {
        BtreeNode newChild = new BtreeNode(nodeCount);
        newChild.setLeaf(oldChild.getLeaf());
        newChild.setPopulation(minimumDegree - 1);
        for(int i = 0; i < (minimumDegree - 1); i++){
            newChild.putKey(i, oldChild.getKey(i + minimumDegree));
            newChild.putFreqs(i, oldChild.getFreqs(i + minimumDegree));
        }
        if(!newChild.getLeaf()){
            for( int i = 0; i <= minimumDegree - 1; i++ ){
                newChild.putChild(i, oldChild.getChild(i + minimumDegree));
            }
        }
        oldChild.setPopulation(minimumDegree - 1);
        for(int i = parent.getPopulation() ; i > index; i-- ){
            parent.putChild(i + 1, parent.getChild(i));
        }
        parent.putChild(index + 1, newChild.index);
        for (int i = parent.getPopulation() - 1; i >= index; i--){
            parent.putKey(i + 1, parent.getKey(i));
            parent.putFreqs(i + 1, parent.getFreqs(i));
        }
        parent.putKey(index, oldChild.getKey(minimumDegree - 1));
        parent.putFreqs(index, oldChild.getFreqs(minimumDegree - 1));
        parent.setPopulation(parent.getPopulation() + 1);
        writeNode(oldChild);
        writeNode(newChild);
        writeNode(parent);
        nodeCount++;
        return newChild;
    }

    public void insert(String key, int url, int freq) throws Exception{
        if(key.length() > 20) key = key.substring(0,20);
        if(! searchAndAdd(root, key, url, freq)) {
            BtreeNode r = root;
            if (r.getPopulation() == (minimumDegree * 2 - 1)) {
                BtreeNode s = new BtreeNode(nodeCount);
                nodeCount++;
                root = s;
                file.seek((long)0);
                file.writeInt(root.index);
                s.putChild(0, r.index);
                splitChild(s, r, 0);
            }
            insertNonFull(root, key, url, freq);
        }
    }

    public void insertNonFull(BtreeNode node, String key, int url, int freq) throws Exception {
        short[] newFreqs;
        if (node.getLeaf()) {
            int i = node.getPopulation() - 1;
            for (; i >= 0 && key.compareTo(new String(node.getKey(i))) < 0; i--) {
                node.putKey(i + 1, node.getKey(i));
                node.putFreqs(i + 1, node.getFreqs(i));
            }
            newFreqs = new short[20];
            newFreqs[url] = (short)freq;
            node.putKey(i + 1, key.toCharArray());
            node.putFreqs(i + 1, newFreqs);
            node.setPopulation((node.getPopulation() + 1));
            writeNode(node);
        }else{
            int i = node.getPopulation() - 1;
            for (; i >= 0 && key.compareTo(new String(node.getKey(i))) < 0; i--);
            i++;
            BtreeNode child = readNode(node.getChild(i));
            if (child.isFull()){
                BtreeNode newChild = splitChild(node, child, i);
                if(key.compareTo(new String(node.getKey(i))) > 0)
                    child = newChild;
            }
            insertNonFull(child, key,  url, freq);
        }
    }

    public BtreeNode readNode(int index)throws Exception {
        file.seek((long) 516 + index * (168 * minimumDegree - 77));
        byte[] bytes = new byte[168 * minimumDegree - 77];
        file.readFully(bytes);
        return new BtreeNode(index, bytes);
    }

    public void writeNode(BtreeNode node) throws Exception {
        file.seek((long) 516 + node.index * (168 * minimumDegree - 77));
        file.write(node.array());
    }

    public void display() throws Exception{
        System.out.println("root: " + root.index);
        System.out.println("nodeCount: " + nodeCount);
        System.out.println();
        for(int i = 0; i < nodeCount; i ++){
            BtreeNode node = readNode(i);
            if(! node.getLeaf())node.display();
        }
    }
}

class BtreeNode{
    int index;
    ByteBuffer b;
    short minimumDegree = Btree.minimumDegree;

    public boolean isFull(){
        return getPopulation()==(short)(2 * minimumDegree - 1) ;
    }

    public boolean getLeaf(){
        b.rewind();
        return b.get()!= 0;
    }

    public void setLeaf(boolean bool){
        if(bool) {
            b.put(0, (byte) 1);
        }else{
            b.put(0, (byte) 0);
        }
    }

    public char[] getKey(int index){
        char[] key = new char[20];
        for(int i = 0; i < 20; i++)
            key[i] = b.getChar(7 + index * 84 + i * 2);
        return key;
    }

    protected void putKey(int index, char [] key){
        b.position(7 + 84 * index);
        for(char letter : key){
            b.putChar(letter);
        }
        for(int i = key.length; i < 20; i ++){
            b.putChar(' ');
        }
    }

    public short[] getFreqs(int index){
        short[] freqs = new short[20];
        for(int i = 0; i < 20; i++)
            freqs[i] = b.getShort(47 + 84 * index + i * 2);
        return freqs;
    }

    protected void putFreqs(int index, short[] freqs){
        b.position(47 + (84 * index));
        for(short freq : freqs){
            b.putShort(freq);
        }
    }

    public void addFreq(int index, int url, int freq){
        short[] freqs = getFreqs(index);
        freqs[url] = (short) (freqs[url] + freq);
        putFreqs(index, freqs);
    }

    public short getPopulation(){
        return b.getShort(1);
    }

    public void setPopulation(int pop){
        b.putShort(1,(short)pop);
    }

    public int getChild(int index){
        return b.getInt(3 + index * 84);
    }
    public void putChild(int index, int link){
        b.putInt(3 + index * 84, link);
    }

    public BtreeNode(int index){
        this.index = index;
        b = ByteBuffer.allocate(168 * minimumDegree - 77);
    }

    public BtreeNode(int index, byte[] bytes){
        b = ByteBuffer.allocate(168 * minimumDegree - 77);
        b.put(bytes);
        this.index = index;
    }

    public byte[] array(){
        return b.array();
    }

    public void display(){
        System.out.println("     index: " + index);
        System.out.println("      leaf: " + getLeaf());
        System.out.println("population: "+ getPopulation());
        int pop = getPopulation();
        for (int i = 0; i < pop ;i++){
            System.out.println("     child: " + getChild(i));
            System.out.println("       key: " + new String(getKey(i)));
            short[] freqs = getFreqs(i);
            System.out.print("     freqs: "+ freqs[0]);
            for(int j = 1; j < freqs.length; j++){
                System.out.print(", " + freqs[j]);
            }
            System.out.println();
        }
        System.out.println("     child: " + getChild(getPopulation()));
        System.out.println("\n");
    }
}

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

class HistIter implements Iterator<HistNode>{
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

