import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by home on 4/20/15.
 */
class Btree  implements Iterable<StringFreqs>{
    RandomAccessFile file;
    int nodeCount;
    BtreeNode root;
    public static final short minimumDegree = 500;
    public static final int urlCount = AdjacencyList.capacity,
            urlLength = 512,//All lengths are in bytes
            urlArrayLength = urlLength * urlCount,
            cacheNodeLength = urlLength + 259,
            cacheBuckets = 0,//This must be a power of 2
            cacheLength = cacheNodeLength * cacheBuckets,
            metadataLength = cacheLength + 4 + urlArrayLength,
            nodeLength = 168 * minimumDegree - 77;
    private static final Set<String> junkWords = new HashSet<String>(Arrays.asList(
            new String[]{"nbsp", "with", "were", "also", "from"}
    ));

    public void clearCache()throws Exception{
        file.seek(0);
        file.write(new byte[cacheLength]);
    }

    private static final Set<String> junkUrls = new HashSet<String>(Arrays.asList(
            new String[]{"http://www.mediawiki.org/",
                    "http://www.wikimediafoundation.org/",
                    "http://en.wikipedia.org/wiki/Help:Category",
                    "http://en.wikipedia.org/wiki/Portal:Current_events",
                    "http://en.wikipedia.org/wiki/Portal:Featured_content",
                    "http://en.wikipedia.org/wiki/Special:Random",
                    "http://en.wikipedia.org/wiki/Portal:Contents",
                    "http://en.wikipedia.org/wiki/Wikipedia:Community_portal",
                    "https://www.mediawiki.org/wiki/Special:MyLanguage/How_to_contribute",
                    "http://wikimediafoundation.org/wiki/Terms_of_Use",
                    "http://en.wikipedia.org/wiki/Wikipedia:General_disclaimer",
                    "http://shop.wikimedia.org",
                    "http://en.wikipedia.org/wiki/Help:Contents",
                    "http://en.wikipedia.org/wiki/Special:SpecialPages",
                    "http://creativecommons.org/licenses/by-sa/3.0/",
                    "http://wikimediafoundation.org/",
                    "http://en.wikipedia.org/wiki/Special:RecentChanges",
                    "http://wikimediafoundation.org/wiki/Privacy_policy",
                    "http://en.wikipedia.org/wiki/Wikipedia:Contact_us",
                    "https://donate.wikimedia.org/wiki/Special:FundraiserRedirector?utm_source=donate&utm_medium=sidebar&utm_campaign=C13_en.wikipedia.org&uselang=en",
                    "http://en.wikipedia.org/wiki/Main_Page",
                    "http://en.wikipedia.org/wiki/Wikipedia:About",
                    "http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License",
                    "http://en.wikipedia.org/wiki/Wikipedia:File_Upload_Wizard",
            }
    ));

    public double[] compare(String url)throws Exception{
        Histogram hist = new Histogram(url);
        double[] dotProducts = new double[urlCount];
        double sumOfSquaredHist = 0.0;
        double[] sumofSquaredBtree = new double[urlCount];
        double[] scores = new double[urlCount];
        for(HistNode histNode : hist){
            insert(histNode.word, 0, 0);
        }
        for(StringFreqs stringFreqs : this){
            String word = stringFreqs.string;
            int freq = hist.getCount(word);
            short[] freqs = stringFreqs.freqs;
            sumOfSquaredHist += Math.pow(freq,2);
            for(int i = 0; i < urlCount; i++){
                sumofSquaredBtree[i] += Math.pow(freqs[i], 2);
                dotProducts[i] += freqs[i] * freq;
            }
        }
        for(int i = 0; i < urlCount; i++){
            scores[i] = dotProducts[i]/(Math.sqrt(sumOfSquaredHist) * Math.sqrt(sumofSquaredBtree[i]));
        }
        return scores;
    }


    public Btree(String name)throws Exception{
        File file = new File(name);
        if(file.isFile()){
            this.file = new RandomAccessFile(file, "rw");
            this.file.seek(cacheLength);
            root = readNode(this.file.readInt());
            int length = (int)this.file.length();
            nodeCount = (int) ((length - metadataLength) / nodeLength);
        }else{
            file.createNewFile();
            this.file = new RandomAccessFile(file, "rw");
            root = new BtreeNode(0);
            root.setLeaf(true);
            writeNode(root);
            nodeCount = 1;
        }
    }

    public void toTextFile() throws Exception{
        int unique = 0;
        File logFile = new File("words.text");
        BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
        writer.write("Root: "+root.index);
        for(StringFreqs freqs : this){
            short[] nums = freqs.freqs;
            writer.write(freqs.string + ": " + nums[0]);
            for(int i = 1; i < urlCount; i++){
                writer.write(", " + nums[i]);
            }
            writer.write("\n");
            unique++;
        }
        writer.write("Unique Words: " + unique);
        writer.close();
    }

    public BtreeIterator iterator(){
        return new BtreeIterator(root, this);
    }

    public void loadWords(String url, int urlIndex) throws Exception{
        //System.out.println("loading " + url);
        URL myURL = new URL(url);
        BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
        String inputLine, doc = "";
        while ((inputLine = in.readLine()) != null) doc += inputLine + " ";
        in.close();
        doc = Jsoup.clean(doc, Whitelist.none());//remove html tags
        doc = doc.replaceAll("[^a-zA-Z ]", " ").toLowerCase();//remove punctuation, to lower case.
        Scanner sc = new Scanner(doc);
        String word;
        while(sc.hasNext()) {
            word = sc.next();
            if(word.length() > 3 && ! junkWords.contains(word)) {
                insert(word, urlIndex, 1);
            }
        }
    }

    public void putUrl(String url, int i)throws IOException{
        file.seek(cacheLength + 4 + (i * urlLength));
        for(int j = 0; j < (urlLength / 2); j++) {
            if (j < url.length()) {
                file.writeChars(url.substring(j, j + 1));
            }else{
                file.writeChars(" ");
            }
        }
    }

    public String getUrl(int i) throws IOException{
        file.seek(cacheLength + 4 + (i * urlLength));
        char[] url = new char[(urlLength/2)];
        for(int j = 0; j < (urlLength/2); j++) {
            url[j] = file.readChar();
        }
        return new String(url).trim();
    }

    public CacheNode getCacheNode(int i)throws Exception{
        CacheNode node;
        char[] url = new char[(urlLength/2)];
        Long dateAccessed;
        byte[] matches = new byte[3];
        double[] scores = new double[3];
        file.seek(i * cacheNodeLength);
        for ( int j = 0; j < (urlLength/2); j++){
            url[j] = file.readChar();
        }
        dateAccessed = file.readLong();
        for(int j = 0; j < 3; j ++){
            matches[j] = file.readByte();
            scores[j] = file.readDouble();
        }
        return new CacheNode(url, matches, scores, dateAccessed);
    }

    public void putCacheNode(CacheNode node, int i)throws Exception{
        file.seek(i * cacheNodeLength);
        char[]url = node.url.toCharArray();
        for(int j = 0; j < (urlLength/2); j++){
            if(j < url.length){
                file.writeChar(url[j]);
            }else{
                file.writeShort(0);
            }
        }
        file.writeLong(node.dateAccessed);
        for(int j = 0; j < 3; j++){
            file.writeByte(node.matches[j]);
            file.writeDouble(node.scores[j]);
        }
    }

    public void histLoadWords(String url, int urlIndex) throws Exception{
        Histogram histogram = new Histogram(url);
        //System.out.println(histogram.uniqueCount);
        for(HistNode histNode: histogram){
            String word = histNode.word;
            if(word.length() > 3 && ! junkWords.contains(word)){
                insert(histNode.word, urlIndex, histNode.count);
            }
        }
    }

    public void loadPage(String url, int urlIndex)throws Exception{
        //System.out.println("loading " + url);
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        String newUrl;
        loadWords(url, urlIndex);
        for (Element link : links) {
            newUrl = link.attr("abs:href");
            if(! junkUrls.contains(newUrl) && ! newUrl.contains(url)){
                try{
                    histLoadWords(newUrl, urlIndex);
                }catch (FileNotFoundException e){
                    //System.out.println("Bad url: " + newUrl);
                }catch (IOException e){
                    //e.printStackTrace();
                }
            }
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
        BtreeNode r = root;
        if (r.getPopulation() == (minimumDegree * 2 - 1)) {
            BtreeNode s = new BtreeNode(nodeCount);
            nodeCount++;
            root = s;
            file.seek(cacheLength);
            file.writeInt(root.index);
            s.putChild(0, r.index);
            splitChild(s, r, 0);
        }
        insertNonFull(root, key, url, freq);
    }

    public void insertNonFull(BtreeNode node, String key, int url, int freq) throws Exception {
        short[] newFreqs;
        int i = node.getPopulation() - 1;
        for (; i >= 0 && key.compareTo(new String(node.getKey(i))) <= 0; i--);
        if(key.equals(new String(node.getKey(i+1)).trim())){
            newFreqs = node.getFreqs(i + 1);
            newFreqs[url] = (short)(newFreqs[url] + freq);
            node.putFreqs(i + 1, newFreqs);
            writeNode(node);
            return;
        }
        if (node.getLeaf()) {
            for (int j = node.getPopulation() - 1; j > i ; j--) {
                node.putKey(j + 1 , node.getKey(j));
                node.putFreqs(j + 1 , node.getFreqs(j));
            }
            newFreqs = new short[urlCount];
            newFreqs[url] = (short)freq;
            node.putKey(i + 1, key.toCharArray());
            node.putFreqs(i + 1, newFreqs);
            node.setPopulation((node.getPopulation() + 1));
            writeNode(node);
        }else{
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
        file.seek((long) metadataLength + index * nodeLength);
        byte[] bytes = new byte[nodeLength];
        file.readFully(bytes);
        return new BtreeNode(index, bytes);
    }

    public void writeNode(BtreeNode node) throws Exception {
        file.seek((long) metadataLength + node.index * nodeLength);
        file.write(node.array());
    }

    public void display() throws Exception{
        System.out.println("root: " + root.index);
        System.out.println("nodeCount: " + nodeCount);
        System.out.println();
        for(int i = 0; i < nodeCount; i ++){
            BtreeNode node = readNode(i);
            if(true)node.display();
        }
    }
}

class BtreeNode{
    int index;
    ByteBuffer b;
    public static final int metadataLength = 3,
            keyLength = 40,
            valueLength = 40,
            pointerLength = 4;

    public static short minimumDegree = Btree.minimumDegree;

    public boolean isFull(){
        return getPopulation()==(short)(2 * minimumDegree - 1) ;
    }

    public boolean getLeaf(){
        b.rewind();
        return b.get()!= 0;
    }

    public BtreeNodeIterator iterator(Btree btree){
        return new BtreeNodeIterator(this, btree);
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
            key[i] = b.getChar(metadataLength + pointerLength + index * (keyLength + pointerLength + valueLength) + i * 2);//
        return key;
    }

    protected void putKey(int index, char [] key){
        b.position(metadataLength + pointerLength + index * (keyLength + pointerLength + valueLength));
        for(char letter : key){
            b.putChar(letter);
        }
        for(int i = key.length; i < 20; i ++){
            b.putChar(' ');
        }
    }

    public short[] getFreqs(int index){
        short[] freqs = new short[Btree.urlCount];
        for(int i = 0; i < Btree.urlCount; i++)
            freqs[i] = b.getShort(metadataLength + pointerLength + keyLength + (keyLength + pointerLength + valueLength) * index + i * 2);
        return freqs;
    }

    protected void putFreqs(int index, short[] freqs){
        b.position(metadataLength + pointerLength + keyLength + ((keyLength + pointerLength + valueLength) * index));
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
        return b.getInt(metadataLength + index * (keyLength + pointerLength + valueLength));
    }
    public void putChild(int index, int link){
        b.putInt(metadataLength + index * (keyLength + pointerLength + valueLength), link);
    }

    public BtreeNode(int index){
        this.index = index;
        b = ByteBuffer.allocate(Btree.nodeLength);
    }

    public BtreeNode(int index, byte[] bytes){
        b = ByteBuffer.allocate(Btree.nodeLength);
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

class StringFreqs{
    public short[] freqs;
    public String string;
    public StringFreqs(String string, short[] freqs){
        this.string = string;
        this.freqs = freqs;
    }
}

class BtreeIterator implements Iterator<StringFreqs> {
    BtreeNodeIterator innerIter;
    public BtreeIterator(BtreeNode node, Btree btree){
        innerIter = node.iterator(btree);
    }
    public boolean hasNext(){
        return innerIter.hasNext();
    }
    public StringFreqs next(){
        return innerIter.next();
    }
}

class BtreeNodeIterator  implements Iterator<StringFreqs> {
    BtreeNodeIterator innerIter;
    BtreeNode node;
    Btree btree;
    int i;
    public BtreeNodeIterator(BtreeNode node, Btree btree){
        i = 0;
        this.node = node;
        this.btree = btree;
        if(!node.getLeaf()){
            try{
                innerIter = btree.readNode(node.getChild(0)).iterator(btree);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public boolean hasNext(){
        if (node.getLeaf()){
            return i < node.getPopulation();
        }else{
            return i < node.getPopulation() || innerIter.hasNext();
        }
    }
    public StringFreqs next(){
        if(node.getLeaf()){
            return new StringFreqs(new String(node.getKey(i)).trim(), node.getFreqs(i++));
        }else{
            if(innerIter.hasNext()){
                return innerIter.next();
            }else if(i < node.getPopulation()){
                try {
                    innerIter = btree.readNode(node.getChild(i + 1)).iterator(btree);
                }catch(Exception e){
                    e.printStackTrace();
                }
                return new StringFreqs(new String(node.getKey(i)).trim(), node.getFreqs(i++));
            }else{
                return null;
            }
        }
    }
}

class CacheNode{
    String url;
    byte[] matches;
    double[] scores;
    long dateAccessed;
    public CacheNode(char[] url, byte[] matches, double[] scores, long dateAccessed){
        this.url = new String(url).trim();
        this.matches = matches;
        this.scores = scores;
        this.dateAccessed = dateAccessed;
    }
}