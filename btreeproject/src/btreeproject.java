/**
 * Created by home on 3/23/15.
 */
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
public class btreeproject {
    public static void main(String[] args) throws Exception {
        Btree btree = new Btree("btree");
        BtreeNode root = btree.readNode(0);
        System.out.println(root.getLeaf());
    }
}

class Btree{
    RandomAccessFile file;
    int nodeCount;
    BtreeNode root;
    public static final short minimumDegree = 3;

    public Btree(String name)throws Exception{
        File file = new File(name);
        if(file.isFile()){
            this.file = new RandomAccessFile(file, "rw");
            root = readNode(this.file.readShort());
            nodeCount = (int) ((this.file.length() - 514) / (168 * minimumDegree + 84));
        }else{
            file.createNewFile();
            this.file = new RandomAccessFile(file, "rw");
            root = new BtreeNode(0);
            root.setLeaf(true);
            writeNode(root);
            nodeCount = 1;
        }
    }

    public void splitChild(BtreeNode parent, BtreeNode oldChild, int index) throws Exception {
        BtreeNode newChild = new BtreeNode(nodeCount);
        newChild.setLeaf(oldChild.getLeaf());
        newChild.setPopulation(minimumDegree - 1);
        for(int i = 0; i < 248; i++){
            newChild.putKey(i, oldChild.getKey(i));
            newChild.putFreqs(i, oldChild.getFreqs(i));
        }
        if(!newChild.getLeaf()){
            for( int i = 0; i < minimumDegree - 1; i++ ){
                newChild.putChild(i, oldChild.getChild(i));
            }
        }
        oldChild.setPopulation(minimumDegree - 1);
        for(int i = parent.getPopulation() ; i >= index; i-- ){
            parent.putChild(i + 1, parent.getChild(i));
        }
        parent.putChild(index + 1, newChild.index);
        for (int i = parent.getPopulation() - 1; i > index; i--){
            parent.putChild(i + 1, parent.getChild(i));
        }
        parent.putKey(index, oldChild.getKey(minimumDegree - 1));
        parent.setPopulation(parent.getPopulation() + 1);
        writeNode(oldChild);
        writeNode(newChild);
        writeNode(parent);
    }

    public void insertNonFull(BtreeNode node, String key, int url, int freq) throws Exception {
        short[] newFreqs;
        if (node.getLeaf()) {
            int i = node.getPopulation() - 1;
            for (; i >= 0 && key.compareTo(new String(node.getKey(i))) < 0; i--) {
                node.putKey(i + 1, node.getKey(i));
                node.putFreqs(i + 1, node.getFreqs(i));
            }
            newFreqs = node.getFreqs(i + 1);
            newFreqs[url] = (short) (newFreqs[url] + freq);
            node.putKey(i + 1, key.toCharArray());
            node.putFreqs(i + 1, newFreqs);
            node.setPopulation(node.getPopulation() + 1);
        }else{
            int i = node.getPopulation() - 1;
            for (; i >= 0 && key.compareTo(new String(node.getKey(i))) < 0; i--);
            i++;
            BtreeNode child = readNode(node.getChild(i));
            if (child.isFull()){
                splitChild(node, child, node.getChild(i));
                if(key.compareTo(new String(node.getKey(i))) > 0)
                    i++;
            }
            insertNonFull(child, key,  url, freq);
        }
    }

    public BtreeNode readNode(int index)throws Exception {
        file.seek((long) 514 + index * (168 * minimumDegree - 77));
        byte[] bytes = new byte[168 * minimumDegree - 77];
        file.readFully(bytes);
        return new BtreeNode(index, bytes);
    }

    public void writeNode(BtreeNode node) throws Exception {
        file.seek((long) 514 + node.index * (168 * minimumDegree - 77));
        file.write(node.array());
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
    }

    public short[] getFreqs(int index){
        short[] freqs = new short[20];
        for(int i = 0; i < 20; i++)
            freqs[i] = b.getShort(47 + 84 * index + i * 2);
        return freqs;
    }

    protected void putFreqs(int index, short[] freqs){
        b.position(47 + 84 * index);
        for(short freq : freqs){
            b.putShort(freq);
        }
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
}