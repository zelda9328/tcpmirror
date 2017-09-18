
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author engin
 */
public class TCPMirror{
    Selector selector;
    ServerSocketChannel lsnChannel;    
    TCPMirror() throws IOException{
        Properties property = new Properties();
        InputStream is = this.getClass().getResourceAsStream("/network.xml");
        property.loadFromXML(is);
        int port = Integer.parseInt(property.getProperty("port", "30001"));
        
        selector = Selector.open();
        
        lsnChannel = ServerSocketChannel.open();
        lsnChannel.socket().bind(new InetSocketAddress(port));
        lsnChannel.configureBlocking(false);
        lsnChannel.register(selector, SelectionKey.OP_ACCEPT , this);
    }
    
    void start(){
        while(!Thread.interrupted()){
            try {
                selector.select();
                Set<SelectionKey> key = selector.selectedKeys();
                for(SelectionKey k : key){
                    if(k.isAcceptable()){
                        SocketChannel s = lsnChannel.accept();
                        s.configureBlocking(false);
                        s.register(selector, SelectionKey.OP_READ, new InnerArg(s));
                    }
                    if(k.isReadable()){
                        InnerArg skt = (InnerArg)k.attachment();
                        ByteBuffer bf = ByteBuffer.allocate(1024);
                        bf.clear();
                        
                        skt.s.read(bf);
                        bf.flip();
                        while(bf.hasRemaining()){
                            skt.s.write(bf);
                        }
                    }
                }
                key.clear();
            } catch (IOException ex) {
                Logger.getLogger(TCPMirror.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    class InnerArg{
        SocketChannel s;
        InnerArg(SocketChannel s){
            this.s = s;
        }
    }
    
    public static void main(String[] args) throws IOException{
        TCPMirror m = new TCPMirror();
        m.start();
    }
}
