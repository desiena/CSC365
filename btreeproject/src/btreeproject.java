/**
 * Created by home on 3/23/15.
 */
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.swing.*;

public class btreeproject {
    Btree btree;
    JButton button;
    JLabel matchOne, matchTwo, matchThree, one, two, three;
    JTextField url;
    JFrame frame = new JFrame("Website Similarity Tester");

    public btreeproject(Btree btree){
        this.btree = btree;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(frame);
            }
        });
    }

    public void addComponentsToPane(Container pane) {
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;


        button = new JButton("Find Matches");
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(button, c);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compare();
            }
        });

        url = new JTextField();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.ipadx=700;
        c.gridx = 1;
        c.gridy = 0;
        pane.add(url, c);
        c.ipadx=0;

        matchOne = new JLabel("Match One: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(matchOne, c);

        matchTwo = new JLabel("Match Two: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 2;
        pane.add(matchTwo, c);

        matchThree = new JLabel("Match Three: ");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 3;
        pane.add(matchThree, c);

        one = new JLabel("");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 1;
        pane.add(one, c);

        two = new JLabel("");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 2;
        pane.add(two, c);

        three = new JLabel("");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 3;
        pane.add(three, c);
    }

    void compare() {
        try {
            CacheNode node = btree.getCacheNode(url.getText().hashCode() & 15);
            if(node.url.equals(url.getText()) && node.dateAccessed > new URL(node.url).openConnection().getLastModified()) {
                one.setText(btree.getUrl((int) node.matches[0]));
                two.setText(btree.getUrl((int) node.matches[1]));
                three.setText(btree.getUrl((int) node.matches[2]));
                matchOne.setText("Match One: " + node.scores[0]);
                matchTwo.setText("Match Two: " + node.scores[1]);
                matchThree.setText("Match Three: " + node.scores[2]);
            }else {
                double[] sims = btree.compare(url.getText());
                for (int i = 0 ; i < 20; i ++){
                    System.out.println( sims[i]);
                }
                byte first = 0, second = 0, third = 0;
                double firstScore = sims[0], secondScore = -1, thirdScore = -1;
                for (byte i = 1; i < 20; i++) {
                    if (sims[i] >= firstScore) {
                        thirdScore = secondScore;
                        secondScore = firstScore;
                        firstScore = sims[i];
                        third = second;
                        second = first;
                        first = i;
                    } else if (sims[i] >= secondScore) {
                        thirdScore = secondScore;
                        secondScore = sims[i];
                        third = second;
                        second = i;
                    } else if (sims[i] >= thirdScore) {
                        thirdScore = sims[i];
                        third = i;
                    }
                }
                one.setText(btree.getUrl(first));
                two.setText(btree.getUrl(second));
                three.setText(btree.getUrl(third));
                matchOne.setText("Match One: " + firstScore);
                matchTwo.setText("Match Two: " + secondScore);
                matchThree.setText("Match Three: " + thirdScore);
                byte[] matches = {first, second, third};
                double[] scores = {firstScore, secondScore, thirdScore};
                btree.putCacheNode(new CacheNode(url.getText().toCharArray(), matches, scores, new Date().getTime()), url.getText().hashCode() & 15);
            }
        } catch (Exception e) {
            one.setText(e.getMessage());
        }
    }


    private void createAndShowGUI(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content pane.
        addComponentsToPane(frame.getContentPane());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        Btree btree = new Btree("btree");
        //btree.load();
        //btree.clearCache();
        btreeproject proj = new btreeproject(btree);
        //btree.toTextFile();
    }
}

class Btree  implements Iterable<StringFreqs>{
    RandomAccessFile file;
    int nodeCount;
    BtreeNode root;
    public static final short minimumDegree = 500;
    private static final Set<String> junkWords = new HashSet<String>(Arrays.asList(
            new String[]{"nbsp", "with", "were", "also", "from"}
    ));

    public void clearCache()throws Exception{
        file.seek(0);
        file.write(new byte[12336]);
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
        double[] dotProducts = new double[20];
        double sumOfSquaredHist = 0.0;
        double[] sumofSquaredBtree = new double[20];
        double[] scores = new double[20];
        for(HistNode histNode : hist){
            insert(histNode.word, 0, 0);
        }
        for(StringFreqs stringFreqs : this){
            String word = stringFreqs.string;
            int freq = hist.getCount(word);
            short[] freqs = stringFreqs.freqs;
            sumOfSquaredHist += Math.pow(freq,2);
            for(int i = 0; i < 20; i++){
                sumofSquaredBtree[i] += Math.pow(freqs[i], 2);
                dotProducts[i] += freqs[i] * freq;
            }
        }
        for(int i = 0; i < 20; i++){
            scores[i] = dotProducts[i]/(Math.sqrt(sumOfSquaredHist) * Math.sqrt(sumofSquaredBtree[i]));
        }
        return scores;
    }


    public Btree(String name)throws Exception{
        File file = new File(name);
        if(file.isFile()){
            this.file = new RandomAccessFile(file, "rw");
            this.file.seek(12336);
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

    public void toTextFile() throws Exception{
        int unique = 0;
        File logFile = new File("words.text");
        BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
        writer.write("Root: "+root.index);
        for(StringFreqs freqs : this){
            short[] nums = freqs.freqs;
            writer.write(freqs.string + ": " + nums[0]);
            for(int i = 1; i < 20; i++){
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
        file.seek(12340 + (i * 512));
        for(int j = 0; j < 256; j++) {
            if (j < url.length()) {
                file.writeChars(url.substring(j, j + 1));
            }else{
                file.writeChars(" ");
            }
        }
    }

    public String getUrl(int i) throws IOException{
        file.seek(12340 + (i * 512));
        char[] url = new char[256];
        for(int j = 0; j < 256; j++) {
            url[j] = file.readChar();
        }
        return new String(url).trim();
    }

    public CacheNode getCacheNode(int i)throws Exception{
        CacheNode node;
        char[] url = new char[256];
        Long dateAccessed;
        byte[] matches = new byte[3];
        double[] scores = new double[3];
        file.seek(i * 771);
        for ( int j = 0; j < 256; j++){
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
        file.seek(i * 771);
        char[]url = node.url.toCharArray();
        for(int j = 0; j < 256; j++){
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
        for (int i = 0; i < 20; i ++){
            //loadPage(pages[i], i);
            putUrl(pages[i], i);
            //System.out.println(getUrl(i));
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
            file.seek((long)12336);
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
            newFreqs = new short[20];
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
        file.seek((long) 22580 + index * (168 * minimumDegree - 77));
        byte[] bytes = new byte[168 * minimumDegree - 77];
        file.readFully(bytes);
        return new BtreeNode(index, bytes);
    }

    public void writeNode(BtreeNode node) throws Exception {
        file.seek((long) 22580 + node.index * (168 * minimumDegree - 77));
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
    short minimumDegree = Btree.minimumDegree;

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

class StringFreqs{
    public short[] freqs;
    public String string;
    public StringFreqs(String string, short[] freqs){
        this.string = string;
        this.freqs = freqs;
    }
}

class BtreeIterator implements Iterator<StringFreqs>{
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