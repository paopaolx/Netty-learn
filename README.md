# IO模型
## IO模型基本介绍
1. IO模型简单理解：就是用什么样的通道进行数据的发送和接收，很大程度上决定了程序通信的性能
2. Java目前支持三种网络编程模型：BIO，NIO，AIO
   1. BIO：传统的同步阻塞，服务器实现模式为一个连接对应一个线程，即客户端有连接请求时服务端就需要启动一个线程进行处理，那么在高并发情况下，就会启动大量的线程，如果连接后，没有进行数据通信，线程就会闲置，造成大量的资源开销。而且线程中进行数据读写的时候会阻塞，直到数据准备好，才进行处理
   2. NIO：同步非阻塞，服务器实现模式为一个线程处理多个连接（请求），即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接通道有IO请求就进行处理【一个线程管理多个连接，或管理多个通道的IO请求，可以减少BIO中大量的闲置线程。因为实际情况下，一个连接建立之后，并不是一直处于活动状态，通过观察者模式实现轮询监听，可以减少闲置线程，并有效利用cpu资源】
   3. AIO（NIO.2）：异步非阻塞，jdk1.7引入的，目前还未得到广泛应用。AIO引入了异步通道的概念，采用了Proactor模式，简化了程序编写，有效的请求才启动线程。它的特点是先由操作系统完成后才通知服务端程序启动线程去处理，一般适用于连接数较多且连接时间较长的应用

## BIO，NIO，AIO适用场景分析
1. BIO适用于连接数较少且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，jdk1.4以前的唯一选择，但程序简单易于理解
2. NIO适用于连接数较多且连接时间较短（轻操作）的架构，比如聊天服务器，弹幕系统，服务器间通讯等。编程比较复杂，JDK1.4开始支持
3. AIO适用于连接数较多且连接时间较长（重操作）的架构，比如相册服务器，充分调用OS参与并发操作，编程比较复杂，JDK1.7开始支持


## BIO模型
### 基本介绍
1. Java BIO 就是传统的java io 编程，其相关的类和接口在 java.io 中
2. BIO（blocking io）：同步阻塞，它可以通过线程池机制进行改善（只是可以实现多个客户端连接服务器）

### BIO基本模型架构
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0003.png)

### BIO编程简单流程
1. 服务器端启动一个 ServerSocket
2. 客户端启动一个Socket，对服务端进行通信。默认情况下服务端需要对每个客户连接建立一个线程与之通信
3. 客户端发出请求后，先咨询服务器是否有线程响应，如果没有，则会等待，或者被拒绝
4. 如果有相应，客户端线程会等待请求结束后，再继续执行

### Java BIO 应用实例
实例说明：
1. 使用BIO模型编写一个服务端程序，监听6666端口，当有客户端连接时，就启动一个线程与之通信
2. 要求使用线程池机制改善，可以连接多个客户端
3. 服务端可以接收客户端发送的数据（telnet方式即可）

```java
/**
* BIO服务端
* 1. 一个客户端连接，对应一个线程
* 2. 服务端启动后，如果没有客户端连接，程序会阻塞在 serverSocket.accept()
* 3. 客户端连接后，如果没有向服务端发送数据，程序会阻塞在 inputStream.read(bytes)
**/
public class BIOServer {
    public static void main(String[] args) throws IOException {
        // 线程池机制
        // 1. 创建一个线程池
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        ServerSocket serverSocket = new ServerSocket(6666);
        System.out.println("服务端启动了");
        while(true){
            // 监听，等待客户端连接【会阻塞】
            System.out.println("等待连接...");
            final Socket socket = serverSocket.accept();
            System.out.println("连接到一个客户端");
            // 2. 如果有客户端连接了，就创建一个线程与之通信
            cachedThreadPool.execute(()->{
                // 与客户端通讯的方法
                handler(socket);
            });
        }
    }

    public static void handler(Socket socket){
        try{
            System.out.println("线程信息 id = "+ Thread.currentThread().getId() + " 名称 = "+Thread.currentThread().getName());
            byte[] bytes = new byte[1024];
            InputStream inputStream = socket.getInputStream(); // 通过socket获取输入流
            while(true){ // 循环读取客户端发送的数据
                System.out.println("read...");
                int read = inputStream.read(bytes);  // 如果通道中没有数据【会阻塞】
                if(read != -1){
                    System.out.println(new String(bytes, 0 , read)); // 输出客户端发送的数据
                }else{
                    break;
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            System.out.println("关闭与客户端的连接");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```
通过开多个cmd窗口：
执行命令``` telnet localhost 6666 ``` 模拟多个客户端与服务端建立网络连接，并发送消息。通过观察服务端消息处理方法的打印，可以确认，BIO处理客户端请求的模式是：一个线程处理一个客户端的连接和IO请求

打印信息：
```shell
服务端启动了
连接到一个客户端
线程信息 id = 12 名称 = pool-1-thread-1
1
2
连接到一个客户端
线程信息 id = 13 名称 = pool-1-thread-2
3
3
3
```

### Java BIO问题分析
- 每个请求都需要创建独立的线程，与对应的客户端进行数据Read，业务处理，数据Write
- 当并发数较大时，需要创建大量的线程来处理链接，系统资源占用较大
- 连接建立后，如果当前线程暂时没有数据可读，则线程就会一直阻塞在Read操作上，造成线程资源浪费


## NIO模型
### Java NIO基本介绍
1. 全称java non-blocking io，是jdk提供的新API，从jdk1.4开始，Java提供了一系列改进的输入/输出新特性，被统称为NIO（即New IO），是同步非阻塞的
2. NIO相关类都被放在 java.nio 包及其子包下，并且对原 java.io 包中的很多类进行了改写
3. NIO有三大核心组成部分：Channel，Buffer，Selector（通道，缓冲区，选择器）
4. NIO是面向缓冲区，或面向块的编程。数据读取到一个它稍后处理的缓冲区中，需要时可以在缓冲区中前后移动，这就增加了处理过程中的灵活性，使用它可以提供非阻塞式的高伸缩性网络
5. Java NIO的非阻塞模式，使一个线程从某通道发送请求或者读取数据。对于非阻塞读，它仅能读到目前可用的数据，如果目前没有数据可用，就什么都不会获取，而不是保持线程阻塞，所以直到数据变得可以读取之前，该线程可以继续做其他的事情。非阻塞写也是如此，一个线程请求写入一些数据到某通道，但不需要等待它完全写入，这个线程同时可以去做别的事情
6. 通俗理解：NIO可以做到用一个线程来处理多个操作。假设有1w个请求过来，根据实际情况，可以分配50或100个线程来处理。不像之前BIO那样，必须配置1w个线程进行处理。
7. HTTP2.0使用了多路复用技术，做到了同一个连接并发处理多个请求，而且并发的数量比HTTP1.1大了好几个数量级

### BIO和NIO的区别
1. BIO以流的方式处理数据，NIO以块的方式处理数据，块I/O的效率比流I/O高很多
2. BIO是阻塞的，NIO是非阻塞的
3. BIO基字节流和字符流进行操作，而NIO基于Channel和Buffer进行操作。数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。Selector用于监听多个通道的事件（比如：连接请求，数据到达等），因此，可以使用单个线程就可以监听多个客户端通道

### NIO三大核心原理示意图
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0008.png)

Selector，Channel和Buffer的关系
1. 每个Channel都会对应一个Buffer
2. 一个Selector对应一个线程，一个线程可以对应多个Channel（连接）
3. 该图反映了有三个Channel注册到Selector上了
4. 程序切换到哪个Channel，是由事件决定的，Event是一个重要的概念
5. Selector会根据不同的事件，在各个通道上切换
6. Buffer就是一个内存块，底层是一个数组，它是双向的
7. 数据的读写是通过Buffer，这个和BIO不同，BIO中要么是输入流，要么是输出流，不能双向，但是NIO的buffer可以读也可以写，需要用flip方法进行切换
8. Channel也是双向的，可以返回底层操作系统的情况，比如Linux，底层操作系统的通道就是双向的

### Buffer缓冲区
Buffer类定义了所有缓冲区都具有的四个属性，来提供关于其所包含的数据元素的信息：
```java
private int mark = -1; // 标记
private int position = 0; // 位置，下一个要被读/写的元素的索引，每次读写缓冲区数据时都会改变该值，为下次读写作准备
private int limit; // 表示缓冲区的当前终点，不能对缓冲区超过极限的位置进行读写操作，且极限是可以修改的
private int capacity; // 容量，即可容纳的最大数据量，在缓冲区创建时被设定，并且不能改变
```
除了boolean类型外，Java中其他基本数据类型都有一个Buffer类型与之对应，最常用的是ByteBuffer（二进制数据），该类的主要方法有：
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0013.png)

### Channel通道
NIO的通道类似于流，但是有些区别：
- 通道可以同时进行读写，而流只能读或者只能写
- 通道可以实现异步读写数据
- 通道可以从缓冲区中读数据，也可以写数据到缓冲区中
- channel管道中不存数据，数据都是存在buffer中的，管道只是一个数据流动的工具，类似厨房洗菜池的下水管，洗菜池子就相当于一个buffer

#### 基本介绍
channel是NIO中的一个接口
```java
public interface Channel extends Closeable{}
```
常用的Channel类有：FileChannel，DatagramChannel，ServerSocketChannel，SocketChannel（ServerSocketChannel类似ServerSocket，SocketChannel类似Socket）
- FileChannel 用于文件的数据读写
- DatagramChanne 用于UDP的数据读写
- ServerSocketChannel，SocketChannel 用于TCP的数据读写

实例1：将一个文件中的数据读出
```java
public static void main(String[] args) throws IOException {
    // 创建一个文件输入流
    File file = new File("E:\\learn\\java-learn\\netty-learn\\netty\\src\\main\\java\\com\\atguigu\\nio\\file01.txt");
    FileInputStream fileInputStream = new FileInputStream(file);
    // 通过fileInputStream获取对应的fileChannel
    FileChannel channel = fileInputStream.getChannel();
    // 创建缓冲区
    ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
    // 将通道的数据读入到缓冲区中
    channel.read(byteBuffer);
    // 将bytebuffer中的字节数据转成String
    System.out.println(new String(byteBuffer.array()));
    // 关闭流
    fileInputStream.close();
}
```

实例2：将一个字符串数据写入到一个文件中
```java
public static void main(String[] args) throws IOException {
    String str = "hello,尚硅谷";
    // 创建一个输出流 --> channel
    FileOutputStream fileOutputStream = new FileOutputStream("E:\\learn\\java-learn\\netty-learn\\netty\\src\\main\\java\\com\\atguigu\\nio\\file01.txt");
    // 通过fileOutputStream获取对应的FileChannel
    FileChannel fileChannel = fileOutputStream.getChannel();
    // 创建一个缓存区ByteBuffer
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    // 将str放入到bytebuffer中（读操作）
    byteBuffer.put(str.getBytes());
    // 将bytebuffer中的数据写入到channel（写操作）
    byteBuffer.flip(); // 读写切换
    fileChannel.write(byteBuffer);
    // 关闭流
    fileOutputStream.close();
}
```

实例3：使用一个Buffer完成文件读取
```java
public static void main(String[] args) throws IOException {
    FileInputStream fileInputStream = new FileInputStream("src/main/1.txt");
    FileChannel fileChannel01 = fileInputStream.getChannel();

    FileOutputStream fileOutputStream = new FileOutputStream("src/main/1.txt");
    FileChannel fileChannel02 = fileOutputStream.getChannel();

    ByteBuffer byteBuffer = ByteBuffer.allocate(512);
    while (true) {
        // 这里有一个重要的操作，将buffer的标识位重置（position=0）
        byteBuffer.clear(); // 清空buffer。如果不复位的话，因为第一次循环读完数据后 position=limit，第二次读的时候 read=0，之后会一直循环read=0
        int read = fileChannel01.read(byteBuffer);
        if(read != -1){
            byteBuffer.flip();
            fileChannel02.write(byteBuffer);
        }else{
            break;
        }
    }

    fileInputStream.close();
    fileOutputStream.close();
}
```

实例3的流程图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0016.png)

#### 关于Buffer和Channel的注意细节和事项
1. ByteBuffer支持类型化的put和get，put放入的是什么数据类型，get就应该使用相应的数据类型来取出，否则可能有BufferUnderflowException异常
   ```java
    public static void main(String[] args) {
        // 创建一个Buffer
        ByteBuffer buffer = ByteBuffer.allocate(64);
        // 类型化方式放入数据
        buffer.putInt(100);
        buffer.putLong(9);
        buffer.putChar('尚');
        buffer.putShort((short) 4);
        buffer.flip(); // 读写切换
        // 取出
        System.out.println(buffer.getInt());
        System.out.println(buffer.getLong());
        System.out.println(buffer.getChar());
        System.out.println(buffer.getShort());
    }
   ```
2. 可以将一个普通Buffer转成一个只读的Buffer
   ```java
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        for (int i = 0; i < 64; i++) {
            buffer.put((byte) i);
        }
        buffer.flip();
        // 得到一个只读的buffer
        ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
        System.out.println(readOnlyBuffer.getClass());
        // 读取
        while (readOnlyBuffer.hasRemaining()){
            System.out.println(readOnlyBuffer.get());
        }
        readOnlyBuffer.put((byte) 2);
    }
   ```
3. NIO还提供了MappedByteBuffer，可以让文件直接在内存（堆外内存）中进行修改，而如何同步到文件由NIO来完成
4. 前面讲的读写操作都是通过一个Buffer来完成的，NIO还支持多个Buffer（即Buffer数组）完成读写操作，即Scattering和Gathering
   ```java
    /**
    * @author lixing
    * @date 2022-04-26 11:26
    * @description
    * Scattering：将数据写入到buffer时，可以采用buffer数组，依次写入（分散）
    * Gathering：从buffer读取数据时，可以采用buffer数组，依次读
    */
    public class ScatteringAndGatheringTest {
        public static void main(String[] args) throws IOException {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(7000);
            // 绑定端口到socket，并启动
            serverSocketChannel.socket().bind(inetSocketAddress);
            // 创建buffer数组
            ByteBuffer[] byteBuffers = new ByteBuffer[2];
            byteBuffers[0] = ByteBuffer.allocate(5);
            byteBuffers[1] = ByteBuffer.allocate(3);
            // 等待客户端连接
            SocketChannel socketChannel = serverSocketChannel.accept();
            int messageLength = 8; // 假定从客户端接收8个字节
            // 循环读取
            while (true){
                int byteRead = 0;
                while(byteRead < messageLength){
                    long read = socketChannel.read(byteBuffers);
                    byteRead += read; // 累计读取的字节数
                    System.out.println("byteRead="+byteRead);
                    // 使用流打印，看看当前的这个buffer的position和limit
                    Arrays.stream(byteBuffers).map(buffer->"position="+buffer.position()+" , limit="+buffer.limit()).forEach(System.out::println);
                }
                // 将所有的buffer进行flip
                Arrays.asList(byteBuffers).forEach(Buffer::flip);
                // 将数据读出显示到客户端
                long byteWrite = 0;
                while(byteWrite < messageLength){
                    long l = socketChannel.write(byteBuffers);
                    byteWrite += l;
                }
                // 将所有的buffer进行clear
                Arrays.asList(byteBuffers).forEach(Buffer::clear);
                System.out.println("byteRead="+byteRead+" byteWrite="+byteWrite+" messagelength="+messageLength);
            }
        }
    }
   ```
   
- <font color="red">将数据从channel中读出到buffer中：channel.read(buffer)</font>
- <font color="red">将数据从buffer写入到channel中：channel.write(buffer)</font>

![](https://img-blog.csdn.net/20151216134020281)

### Selector选择器
#### 基本介绍
1. Java的NIO，用非阻塞的IO方式。可以用一个线程，处理多个客户端连接，就使用到了Selector选择器
2. Selector能够检测多个注册的通道上是否有事件发生（多个Channel以事件的方式可以注册到同一个Selector上）。如果有事件发生，便获取事件，然后针对每个事件进行相应的处理。这样就可以只用一个线程去管理多个通道，也就是管理多个连接和请求。
3. 只有在连接通道真正有读写事件发生时，才会进行读写，大大减少了系统的开销，并且不用每个连接都创建一个线程，不用去维护多个线程
4. 避免了多线程之间的上下文切换导致的开销

#### 特点再说明
1. Netty的IO线程NioEventLoop聚合了Selector，可以同时并发处理成百上千的客户端连接
2. 当线程从某客户端Socket通道进行读写数据时，若没有数据可用时，线程可以进行其他任务的处理（不用阻塞等待）
3. 线程通常将非阻塞IO的空闲时间用于用于在其他通道上执行IO操作，所以单独的线程可以管理多个输入和输出通道
4. 由于读写操作都是非阻塞的，这就可以充分提升IO线程的运行效率，避免由于频繁IO阻塞导致的线程挂起
5. 一个IO线程可以并发处理N个客户端连接和读写操作，这从根本上解决了传统BIO一个连接对应一个线程的模型。架构的性能、弹性伸缩能力和可靠性都得到了极大的提升

#### Selector API 介绍
Selector是一个抽象类，常用的方法和说明如下：
```java
public abstract class Selector implements Closeable {
    public static Selector open(); // 得到一个selector对象
    public int select(); // 监听所有注册的channel，当其中有IO操作可以进行时，将对应的SelectionKey加入到内部集合中并返回 【不带参数的select方法是一个阻塞方法，它会阻塞直到获取到一个有IO操作的channel】
    public int select(long timeout); // 可设置超时，即实现非阻塞
    public int selectNow(); // 如果当前没有任何channel有IO操作发生，则快速返回0，即实现非阻塞
    public Set<SelectionKey> selectedKeys(); // 从内部集合中得到所有的SelectionKey ，每个key即对应一个channel【即返回注册到selector上的所有channel对应的key】
}
```
注意事项：
1. NIO中的ServerSocketChannel功能类似于ServerSocket，SocketChannel功能类似于Socket
2. selector相关方法说明
   - selector.select(); // 阻塞
   - selector.select(1000); // 阻塞1000ms，在1000ms后返回
   - selector.wakeup(); // 唤醒selector
   - selector.selectNow(); // 不阻塞，立马返回

### NIO非阻塞网络编程原理分析图
NIO非阻塞网络编程相关的（Selector，SelectionKey，ServerSocketChannel，SocketChannel）关系梳理图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0019.png)

图例说明：
1. 当客户端连接时，会通过ServerSocketChannel得到SocketChannel
2. Selector通过select()方法进行监听，返回有事件发生的channel个数
3. 通过 register(Selector sel, int ops) 方法将SocketChannel注册到Selector上。一个selector上可以注册多个SocketChannel
4. 注册后会返回一个SelectionKey，会与Selector通过集合的方式建立关联
5. 进一步获取到各个有事件发生的channel对应的SelectionKey
6. 再通过SelectionKey反向获取SocketChannel，通过channel()方法实现
7. 可以通过得到的channel，完成业务处理

```java
/**
 * @author lixing
 * @date 2022-04-26 18:01
 * @description NIO客户端
 */
public class NIOServer {
    public static void main(String[] args) throws IOException {
        // 服务端channel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 服务端channel设置成非阻塞
        serverSocketChannel.configureBlocking(false);
        // 服务端channel绑定网络监听端口
        serverSocketChannel.socket().bind(new InetSocketAddress(6666));
        // 通过Selector的open方法获取到一个Selector实例
        Selector selector = Selector.open();
        // 将服务端channel注册到selector上，关注客户端连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 循环监听客户端连接
        while(true){
            // 通过selector的select(long timeout)方法，设置超时1s去检测是否有客户端通道事件发生
            if(selector.select(1000) == 0){ // 如果等待1s后还是没有任何事件发生，则打印（非阻塞，程序可以做其他事情）
                System.out.println("服务器等待1秒，暂无客户端连接...");
                continue;
            }
            // 如果select方法返回值大于0，表示有事件发生。先获取到所有有事件的selectionKeys集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            // 迭代遍历key，通过key的操作类型进行不同的处理（accept，read）
            while(selectionKeyIterator.hasNext()){
                SelectionKey key = selectionKeyIterator.next();
                if(key.isAcceptable()){ // 如果channel上发生的是客户端连接成功的事件
                    // 获取到对应的客户端channel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    // 设置客户端channel为非阻塞
                    socketChannel.configureBlocking(false);
                    System.out.println("客户端连接成功，客户端channel："+socketChannel.hashCode());
                    // 将客户端channel注册到selector上，关注客户端channel读的事件
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                }
                if(key.isReadable()){ // 如果channel上发生的是数据读的事件（也就是客户端向服务端发送数据了）
                    // 获取到发生事件的客户端channel
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    // 将客户端channel中的数据读出到buffer
                    socketChannel.read(buffer);
                    System.out.println("收到客户端"+socketChannel.hashCode()+"发送的数据："+new String(buffer.array()));
                }
                // 删除当前key，防止重复操作
                selectionKeyIterator.remove();
            }
        }
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-04-26 18:25
 * @description NIO客户端
 */
public class NIOClient {
    public static void main(String[] args) throws IOException {
        // 客户端channel
        SocketChannel socketChannel = SocketChannel.open();
        // 设置客户端channel为非阻塞
        socketChannel.configureBlocking(false);
        // 获取服务端的连接ip和port
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 6666);
        // 与服务端建立连接
        if(!socketChannel.connect(inetSocketAddress)){ // 连接需要时间，客户端不会阻塞
            if(!socketChannel.finishConnect()){ // 如果连接失败，可以做其他的操作
                System.out.println("因为连接需要时间，客户端不会阻塞，可以做其他工作...");
            }
        }
        // 如果连接成功，则通过客户端channel向服务端发送数据
        String str = "hello，尚硅谷~";
        socketChannel.write(ByteBuffer.wrap(str.getBytes()));
        System.in.read();
    }
}
```

输出信息：
```shell
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
客户端连接成功，客户端channel：1338668845
收到客户端1338668845发送的数据：hello，尚硅谷~                                                        
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
服务器等待1秒，暂无客户端连接...
```

#### SelectionKey
1. 相关方法
   ```java
    public abstract class SelectionKey {
        public abstract Selector selector(); // 得到与之关联的Selector对象
        public abstract SelectableChannel channel(); // 得到与之关联的通道
        public final Object attachment(); // 得到与之关联的共享数据
        public abstract SelectionKey interestOps(int ops); // 设置或改变监听事件
        public final boolean isAcceptable(); // 是否可以accept（建立连接）
        public final boolean isReadable(); // 是否可以读
        public final boolean isWritable(); // 是否可以写
    }
   ```
2. SelectionKey：表示Selector与网络通道的注册关系，一共有四种：
   1. int OP_ACCEPT：有新的网络可以accept，值为16
   2. int OP_CONNECT：代表连接已经建立，值为8
   3. int OP_READ：代表读操作，值为1
   4. int OP_WRITE：代表写操作，值为4
   ```java
    public static final int OP_ACCEPT = 1 << 4;
    public static final int OP_CONNECT = 1 << 3;
    public static final int OP_READ = 1 << 0;
    public static final int OP_WRITE = 1 << 2;
   ```

#### ServerSocketChannel
1. ServerSocketChannel在服务端监听新的客户端Socket连接
2. 相关方法
   ```java
    public abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel {
        public static ServerSocketChannel open(); // 得到一个ServerSocketChannel通道
        public final ServerSocketChannel bind(SocketAddress local); // 配置ServerSocketChannel通道的网络监听端口
        public final SelectableChannel configureBlocking(boolean block); // 设置通道的阻塞模式（true 阻塞  ，false 非阻塞）
        public abstract SocketChannel accept(); // 接收一个连接，返回代表这个连接的socket通道对象
        public final SelectionKey register(Selector sel, int ops); // 注册通道到指定的Selector选择器上，并设置监听事件。返回一个SelectionKey
    }
   ```

#### SocketChannel
1. SocketChannel，网络IO通道，具体负责进行读写操作。NIO把缓冲区的数据写入到通道，或者把通道中的数据读出到缓冲区
2. 相关方法
   ```java
    public abstract class SocketChannel extends AbstractSelectableChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
        public static SocketChannel open(); // 得到一个SocketChannel通道
        public final SelectableChannel configureBlocking(boolean block); // 设置SocketChannel通道的阻塞模式（true 阻塞  ，false 非阻塞）
        public abstract boolean connect(SocketAddress remote); // 连接服务器
        public abstract boolean finishConnect(); // 如果上面的方法连接失败，接下来就要通过该方法完成连接操作
        public abstract int write(ByteBuffer src); // 往通道中写入数据
        public abstract int read(ByteBuffer dst); // 从通道中读出数据
        public final void close(); // 关闭通道
    }
   ```

### NIO网络编程应用实例 —— 群聊系统
实例要求：
1. 编写一个NIO群聊系统，实现服务器端和客户端之间的数据简单通讯（非阻塞）
2. 实现多人群聊（客户端之间的消息通讯，都是通过服务器端转发实现的）
3. 服务端：可以监测用户上线，离线，并实现消息转发功能
4. 客户端：通过channel可以无阻塞发送消息给其他用户，同时可以接收其他用户发送的消息
5. 目的：进一步理解NIO非阻塞网络编程机制

代码实现步骤：
1. 编写服务端
   1. 服务端启动并监听6667端口
   2. 服务端接收客户端消息，并实现转发（还需处理客户端上下线）
2. 编写客户端
   1. 连接服务器
   2. 发送消息
   3. 接收消息

```java
/**
 * @author lixing
 * @date 2022-04-27 11:27
 * @description 群聊系统服务端
 */
public class NIOChatServer {
    // 定义属性
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    // 构造器，初始化工作
    public NIOChatServer(){
        try {
            // 获取选择器
            selector = Selector.open();
            // 获取服务端channel
            listenChannel = ServerSocketChannel.open();
            // 设置服务端网络通道监听端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置服务端channel为非阻塞
            listenChannel.configureBlocking(false);
            // 将服务端channel注册到selector上，监听客户端连接事件
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // 监听方法
    public void listenHandler(){
        try {
            // 循环监听客户端事件
            while (true){
                int count = selector.select(); // 阻塞监听客户端通道是否有事件发生
                if(count > 0){ // 说明有事件发生
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        if(key.isAcceptable()){ // 监听到连接事件，将连接事件的通道注册到selector上
                            SocketChannel socketChannel = listenChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println("客户端 "+socketChannel.getRemoteAddress() + " 上线了...");
                        }
                        if(key.isReadable()){ // 监听到读事件
                            readClientData(key);
                        }
                        iterator.remove(); // 移除当前SelectionKey，防止重复操作
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // 读取客户端消息
    public void readClientData(SelectionKey key) throws IOException {
        SocketChannel channel = null;
        try{
            // 获取到发生读事件的channel
            channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // 将通道中数据读出到buffer
            int read = channel.read(buffer);
            if(read > 0){
                String msg = new String(buffer.array());
                System.out.println("接收到客户端 "+channel.getRemoteAddress()+" 消息："+msg);
                // 将消息转发给其他客户端
                sendMsgToOtherClients(msg, channel);
            }
        }catch (IOException e){
            System.out.println("客户端 "+channel.getRemoteAddress()+" 离线...");
            // 取消注册，关闭通道
            key.cancel();
            channel.close();
        }
    }

    // 转发消息给其他客户端
    public void sendMsgToOtherClients(String msg, SocketChannel self) throws IOException {
        // 转发消息的时候要排除自己
        System.out.println("服务器转发消息...");
        // 遍历所有注册到selector上的socketChannel，并排除自己
        for(SelectionKey key: selector.keys()){
            Channel channel = key.channel();
            // 因为注册到selector上的channel还有服务端的ServerSocketChannel
            if(channel instanceof SocketChannel && channel != self){
                ((SocketChannel) channel).write(ByteBuffer.wrap(msg.getBytes()));
            }
        }
    }

    public static void main(String[] args) {
        // 启动服务端
        NIOChatServer nioChatServer = new NIOChatServer();
        nioChatServer.listenHandler();
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-04-27 11:28
 * @description 群聊系统客户端
 */
public class NIOChatClient {
    // 定义属性
    private final String HOST = "127.0.0.1";
    private final int PORT = 6667;
    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    // 构造器，进行初始化操作
    public NIOChatClient() throws IOException {
        // 获取selector对象
        selector = Selector.open();
        // 与服务端建立连接
        socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
        // 设置客户端通道为非阻塞
        socketChannel.configureBlocking(false);
        // 通道注册到selector上，关注读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 获取当前客户端名称
        username = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(username+" is ok... ");
    }

    // 向服务器发送消息
    public void sendMsgToServer(String msg){
        msg = username + "说：" + msg;
        try {
            socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取从服务端发送过来的消息
    public void readMsgFromServer(){
        try {
            int count = selector.select();
            if(count > 0){ // selector上有发生事件的通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if(key.isReadable()){ // 有读操作
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        // 将通道中的数据读出到buffer
                        socketChannel.read(buffer);
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // 启动服务端
        NIOChatClient nioChatClient = new NIOChatClient();
        new Thread(()->{
            // 间隔2秒读取服务端发送过来的消息
            while (true){
                nioChatClient.readMsgFromServer();
                try {
                    Thread.currentThread().sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        // 发送消息给服务端
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()){
            String str = scanner.nextLine();
            nioChatClient.sendMsgToServer(str);
        }
    }
}
```

### NIO与零拷贝
> 零拷贝是网络编程的关键，很多性能优化都离不开零拷贝。所谓零拷贝，从操作系统角度看，是指没有cpu拷贝，只有DMA拷贝
#### 传统IO
- 传统IO会经历：4次拷贝（硬盘 --> 内核buffer --> 用户buffer --> socket buffer --> 协议栈），4次状态切换。
- DMA（Direct Memory Access）拷贝指的是直接内存拷贝，拷贝过程不适用CPU

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0024.png)
#### mmap优化
- mmap通过内存映射，将文件映射到内核缓冲区，同时，用户空间可以共享内核空间的数据。这样在进行网络传输时，就可以减少内核空间到用户空间的拷贝次数。
- 即3次拷贝，4次状态切换

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0025.png)

#### sendFile优化
- Linux2.1提供了sendFile函数，基本原理：数据根本不经过用户态，直接从内核缓冲区进入到socket buffer。同时，由于与用户态完全无关，就减少了一次上下文切换
- 即3次拷贝，3次切换

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0026.png)

#### 零拷贝
- Linux2.4版本中，做了一些修改，避免了从内核缓冲区拷贝到socket buffer的操作，直接拷贝到协议栈，从而再一次减少了数据拷贝。即2次拷贝，3次状态切换
- 其实这里还是由一次cpu拷贝 kernel buffer -> socket buffer。但是，拷贝的信息很少，比如length，offset，一些描述信息的拷贝，消耗很低，可以忽略

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_001/0027.png)

#### 总结
1. 我们常说的零拷贝，是从操作系统的角度来说的。因为内核缓冲区之间，没有数据是重复的（只有 kernel buffer 中有一份数据）
2. 零拷贝不仅仅带来更少的数据复制，还能带来其他的性能优势，例如更少的上下文切换，更少的cpu缓存伪共享，以及无cpu校验和计算

#### 零拷贝案例
案例描述：
1. 使用传统IO的方式传递一个大文件
2. 使用NIO零拷贝方式传递一个大文件（transferTo）
3. 比较两种传输方式消耗的时间
```java
/**
 * @author lixing
 * @date 2022-04-27 15:39
 * @description 传统IO拷贝，服务端
 */
public class OldIOServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7001);
        while (true){
            Socket socket = serverSocket.accept();
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            try{
                byte[] byteArray = new byte[4096];
                while (true){
                    int readCount = dataInputStream.read(byteArray, 0, byteArray.length);
                    if(-1 == readCount){
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

/**
 * @author lixing
 * @date 2022-04-27 15:40
 * @description 传统IO拷贝，客户端
 */
public class OldIOClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 7001);
        String fileName = "src/main/test.zip";
        InputStream inputStream = new FileInputStream(fileName);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[4096];
        long readCount;
        long total = 0;
        long startTime = System.currentTimeMillis();
        while((readCount = inputStream.read(buffer)) >= 0){
            total += readCount;
            dataOutputStream.write(buffer);
        }
        System.out.println("发送总字节数："+total+" ，耗时："+(System.currentTimeMillis()-startTime)); // 发送总字节数：3575330 ，耗时：20
        dataOutputStream.close();
        inputStream.close();
        socket.close();
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-04-27 17:03
 * @description NIO零拷贝，传输文件，服务端
 */
public class NewNIOServer {
    public static void main(String[] args) throws IOException {
        InetSocketAddress address = new InetSocketAddress(7001);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(address);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        while(true){
            SocketChannel socketChannel = serverSocketChannel.accept();
            int readCount = 0;
            while(-1 != readCount){
                try{
                    readCount = socketChannel.read(byteBuffer);
                }catch (Exception e){
                    e.printStackTrace();
                }
                byteBuffer.rewind(); // 倒带 position=0, mark=-1作废
            }
        }
    }
}

/**
 * @author lixing
 * @date 2022-04-27 17:07
 * @description NIO零拷贝，传输文件，客户端
 */
public class NewIOClient {
    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 7001));
        String fileName = "src/main/test.zip";
        FileChannel fileChannel = new FileInputStream(fileName).getChannel();
        long startTime = System.currentTimeMillis();
        // 在linux下，一个transferTo方法就可以传输完成
        // 在windows下一次调用transferTo只能发8MB，如果传输文件过大，需要分段传输，记录每次传输的位置
        // transferTo底层使用到零拷贝
        long transferCount = fileChannel.transferTo(0, fileChannel.size(), socketChannel);
        System.out.println("发送的总的字节数="+transferCount+" 耗时："+(System.currentTimeMillis()-startTime)); // 发送的总的字节数=3575330 耗时：6
        fileChannel.close();
        socketChannel.close();
    }
}
```

```java
/**
* <p> This method is potentially much more efficient than a simple loop
* that reads from this channel and writes to the target channel.  Many
* operating systems can transfer bytes directly from the filesystem cache
* to the target channel without actually copying them.  </p>
**/
public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;
```


## AIO
### Java AIO基本介绍
1. JDK1.7引入了Asynchronous I/O，即AIO。在进行io编程时，常用到两种模式：Reactor和Proactor。Java的NIO就是Reactor，当有事件触发时，服务端得到通知，进行相应的处理。
2. AIO即NIO2.0，异步非阻塞IO。AIO引入了异步通道的概念，采用了Proactor模式，简化了程序编写，有效的请求才启动线程，它的特点是先由操作系统完成后才通知服务端程序启动线程去处理，一般适用于连接数较多且连接时间较长的应用
3. 目前，AIO还没有广泛应用。Netty也是基于NIO，而不是AIO，想详细了解AIO可以查看：[Java新一代网络编程模型AIO原理及Linux系统AIO介绍](http://www.52im.net/thread-306-1-1.html)

## BIO，NIO，AIO比较
| |BIO|NIO|AIO|
|---|---|---|---|
|IO模型|同步阻塞|同步非阻塞（多路复用）|异步非阻塞|
|编程难度|简单|复杂|复杂|
|可靠性|差|好|好|
|吞吐量|低|高|高|
|||||

举例说明：
1. 同步阻塞：到理发店理发，就一直等理发师，直到轮到自己理发
2. 同步非阻塞：到理发店理发，发现前面由其他人理发，给理发师说下，先干其他事情，一会儿过来看是否轮到自己
3. 异步非阻塞：给理发师打电话，让理发师上门服务，自己干其他的事情，理发师自己来家里给你理发

</br>
</br>

# Netty
## 原生NIO存在的问题
1. NIO的类库和API繁杂，使用麻烦，需要熟练掌握Selector，ServerSocketChannel，SocketChannel，ByteBuffer等
2. 需要具备其他的额外技能，要熟悉Java多线程编程，因为NIO编程涉及到Reactor模式（反应器模式），你必须对多线程和网络编程非常熟悉，才能编写出高质量的NIO程序
3. 开发工作量和难度都非常大：例如客户端面临断连重连，网络闪断，半包读写，失败缓存，网络拥塞和异常流的处理等
4. JDK NIO 的bug：例如Epoll bug，它会导致Selector空轮询，最终导致cpu 100%。直到JDK1.7版本该问题依旧存在，没有被根本解决

## Netty的介绍
> Netty is an asynchronous event-driven network application framework for rapid development of maintainable high performance protocol servers & clients.
1. Netty是由JBOSS提供的一个Java开源框架，目前在Github上维护。[官网：https://netty.io](https://netty.io)
2. Netty可以帮助你快速，简单的开发一个网络应用，相当于简化和流程化了NIO的开发过程
3. Netty是目前最流行的NIO框架，Netty在互联网领域，大数据分布式计算领域，游戏行业，通信行业获得了广泛的应用。知名的Elasticsearch、Dubbo框架内部都采用了Netty
4. Netty是一个异步的，基于事件驱动的网络应用框架，可以用于快速开发高性能，高可靠性的网络IO程序
5. Netty主要针对在TCP协议下，面向Clients端的高并发应用，或者Peer-to-Peer场景下的大量数据持续（实时）传输的应用
6. Netty本质是一个NIO框架，适用于服务器通讯相关的多种应用场景
7. 要透彻理解Netty，必须先学习NIO，方便阅读Netty源码

<font color="red">总结：Netty是一个异步的，基于事件驱动的高性能NIO网络通信框架，可以实现服务之间高效，实时的网络通信</font>

## Netty的优点
> Netty对JDK自带的NIO的API进行了封装，解决了上述问题
1. 设计优雅：适用于各种传输类型的统一，API阻塞和非阻塞Socket，基于灵活且可扩展的事件模型，可以清晰地分离关注点，高度可定制的线程模型 -单线程，一个或多个线程池
2. 使用方便：详细记录的Javadoc，用户指南和示例；没有其他依赖项，JDK1.5（Netty 3.x）或JDK1.6（Netty 4.x）就足够了
3. 高性能，高吞吐量，更低延迟，减少资源消耗，最小化不必要的内存复制
4. 安全：完整的SSL/TLS和StartTLS支持
5. 社区活跃，不断更新：社区活跃，版本迭代周期短，发现的bug可以被及时修复，同时，更多的新功能会被加入
6. 目前推荐使用的稳定版本是 Netty 4.x

## Netty的应用场景
1. 互联网行业
    - 在分布式系统中，各个节点之间需要远程服务调用，高性能的RPC框架必不可少，Netty作为异步高性能的通信框架，往往作为基础通信组件被这些RPC框架使用
    - 典型的应用有：阿里的分布式服务框架Dubbo，它的RPC框架使用Dubbo协议进行节点之间的网络通信，Dubbo协议默认使用Netty作为基础通信组件，用于实现各进程节点之间的内部通信
2. 游戏行业
    - Netty作为高性能的基础通信组件，提供了TCP/UDP 和 HTTP 协议栈，方便定制开发私有协议栈，账号登陆服务器
    - 地图服务器之间可以方便的通过Netty进行高性能通信
3. 大数据领域
    - 经典的Hadoop的高性能通信和序列化组件（Avro实现数据文件共享）的RPC框架，默认采用Netty进行跨界点通信
    - 它的Netty Service 基于Netty框架二次封装实现

## Netty线程模型
![Netty架构图](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0001.png)
### 线程模型
目前存在的线程模型有：
1. 传统阻塞I/O服务模型
2. Reactor模式

根据Reactor的数量和处理资源池线程的数量的不同，主要有3种典型的实现：
- 单Reactor，单线程
- 单Reactor，多线程
- 主从Reactor，多线程

Netty线程模型（主要基于主从Reactor多线程模型做了一定的改进，其中主从Reactor多线程模型中有多个Reactor）

#### 传统阻塞I/O服务模型
工作原理图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0002.png)

模型特点
1. 采用阻塞IO模式获取输入的数据
2. 每个连接都需要独立的线程完成完成数据的输入，业务处理，数据返回

问题分析
1. 当并发数很大，就会创建大量的线程，占用很大系统资源
2. 连接创建后，如果当前线程暂时没有数据可读，该线程会阻塞在read操作，造成线程资源浪费

#### Reactor模式
针对传统阻塞I/O服务模型的2个缺点，解决方案：
1. 基于I/O复用模型：多个连接共用一个阻塞对象，应用程序只需要在一个阻塞对象等待，无需阻塞等待所有的连接。当某个连接有新的数据可以处理时，操作系统通知应用程序，线程从阻塞状态返回，开始进行业务处理
2. 基于线程池复用线程资源：不必再为每个连接创建线程，将连接完成后的业务处理任务分配给线程进行处理，一个线程可以处理多个连接的业务

整体设计思想图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0004.png)

说明：
1. Reactor模式，通过一个或多个输入同时传递给服务处理器的模式（基于事件驱动）
2. 服务端程序处理传入的多个请求，并将它们同步分派到相应的处理线程，因此Reactor模式也叫Dispatcher模式
3. Reactor模式使用IP复用监听事件，收到事件后，分发给某个线程，这点就是网络服务器高并发处理的关键

Reactor模式中核心组成：
1. Reactor：Reactor在一个单独的线程中运行，负责监听和分发事件，分发给适当的处理程序来对IO事件作出反应。就像公司的电话接线员，他接听来自客户的电话并将线路转移到适当的联系人
2. Handlers：处理程序执行I/O事件要完成的实际事件，类似于客户想要与之交谈的公司中的实际官员。Reactor通过调度适当的处理程序来响应I/O事件，处理程序执行非阻塞操作

#### 单Reactor单线程
原理图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0005.png)

优缺点
- 优点：模型简单，没有多线程，进程通信，竞争的问题，全部都在一个线程中完成
- 缺点：
  - 性能问题：只有一个线程，无法有效发挥多核cpu的性能。handler在处理某个连接上的业务时，整个线程无法处理其他的连接事件，很容易导致性能瓶颈。
  - 可靠性问题：线程意外终止，或者进入死循环，会导致整个系统通信模块不可用，不能接收和处理外部消息，造成节点故障

#### 单Reactor多线程
原理图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0006.png)

优缺点
- 优点：可以充分的利用多核cpu的处理能力
- 缺点：多线程数据共享和访问比较复杂，reactor处理所有的事件的监听和响应，是采用单线程处理的，在高并发场景容易出现性能瓶颈

#### 主从Reactor多线程
原理图

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0007.png)

优缺点：
- 优点：父线程和子线程的数据交互简单，职责明确，父线程只需要接收新连接，子线程完成后续的业务处理。Reactor主线程只需要把新连接传给子线程，子线程无需返回数据。
- 缺点：编程复杂度高

结合实例：这种模型在许多项目中广泛应用。包括Nginx主从Reactor多进程模型，Memcached主从多线程，Netty主从多线程模型

#### Reactor模式小结
简单化理解：
1. 单Reactor单线程：前台接待员和服务员是同一个人，全称为客户服务
2. 单Reactor多线程：1个前台接待，多个服务员，接待员只负责接待客户，具体的服务由服务员提供
3. 主从Reactor多线程：多个前台接待，多个服务员

Reactor模式具有如下优点：
1. 响应快，不必为单个同步事件所阻塞，虽然Reactor本身依然是同步的
2. 可以最大程度的避免复杂的多线程及同步问题，并且避免了多线程/进程的切换开销
3. 扩展性好，可以方便的通过增加Reactor实例个数来充分利用cpu资源
4. 复用性好，Reactor模型本身与具体事件的处理逻辑无关，具有很高的复用性

## Netty模型
### 工作原理 - 简单版
1. BossGroup线程维护selector，只关注accept
2. 当接收到accept事件后，获取到对应的SocketChannel，封装成NIOSocketChannel，并注册到WorkerGroup（事件循环）的selector中，进行维护
3. 当WorkerGroup线程监听到selector上注册的通道中发生了读写事件后，就进行处理（交给handler处理）。handler已经添加到通道中了

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0010.png)

### 工作原理 - 进阶版

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0011.png)

### Netty工作原理图 - 详细版
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0012.png)

### Netty代码实现
```java
/**
 * @author lixing
 * @date 2022-04-29 15:03
 * @description Netty服务端
 */
public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        // 创建BossGroup和WorkerGroup
        // bossGroup只处理连接请求，真正与客户端进行的业务处理，交给workerGroup完成。这两个都是无限循环
        // bossGroup和workerGroup默认含有的子线程（NioEventLoop）个数为 cpu核数*2
        // 可以自定义设置bossGroup和workerGroup的NioEventLoop线程数，如果workerGroup设置子线程数为8，则如果有超过8个客户端连接，会按照1，2，3...，8，1，2...的方式循环分配
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端启动对象，配置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 使用链式编程来进行设置
            serverBootstrap.group(bossGroup, workerGroup) // 设置两个线程组
                    .channel(NioServerSocketChannel.class) // 使用NioServerSocketChannel作为服务器端的通道实现类（反射）
                    .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列，得到连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道初始化对象（匿名对象）
                        // 给pipeline设置处理器
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new NettyServerHandler()); // 向管道最后追加一个处理器
                        }
                    }); // 给workerGroup的EventLoop对应的管道设置处理器

            System.out.println("服务器 is ok...");
            ChannelFuture cf = serverBootstrap.bind(6668).sync(); // 绑定端口，并且同步，生成了一个ChannelFuture对象

            cf.channel().closeFuture().sync(); // 对关闭通道进行见监听
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

/**
 * @author lixing
 * @date 2022-04-29 15:38
 * @description 自定义一个处理器，需要继承netty规定好的某个HandlerAdapter
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    // 读事件（读取客户端发送的数据）
    // ctx 上下文对象，含有 管道pipeline，一个管道里会有很多个业务处理的handler，通道channel，地址
    // msg 客户端发送的数据，是Object格式的
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("server ctx="+ctx);
        System.out.println("看看channel与pipeline的关系"); // channel与pipeline是相互包含的关系，ctx中包含channel和pipeline，以及其他的信息（大部分的信息都囊括在ctx中）
        Channel channel = ctx.channel();
        ChannelPipeline pipeline = ctx.pipeline(); // 本质是一个双向链表
        // 将msg转成一个ByteBuf
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("接收到客户端 "+ctx.channel().remoteAddress()+" 发送的消息："+buf.toString(CharsetUtil.UTF_8));
    }

    // 读事件完毕，发送数据回复给客户端
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // write + flush 将数据写入到缓存，并刷新
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端 😵 喵", CharsetUtil.UTF_8));
    }

    // 发生异常，则关闭通道
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-04-29 16:19
 * @description Netty客户端
 */
public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyClientHandler());
                        }
                    });
            System.out.println("客户端 is ok...");
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 6668).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}

/**
 * @author lixing
 * @date 2022-04-29 17:24
 * @description
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    // 当通道就绪时，会触发
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client ctx="+ctx);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello，Server 😵 喵", CharsetUtil.UTF_8));
    }

    // 当通道有读取事件时，会触发
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("收到服务器 "+ctx.channel().remoteAddress()+" 回复的消息；"+buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
```

### 任务队列中task有三种典型的应用场景
1. 用户程序自定义的普通任务
   ```java
    // NettyServerHandler.java中
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 如果这里有一个非常耗时的业务处理，就会一直阻塞在这里 (服务端阻塞)

        // 解决方案1：用户程序自定义的普通任务
        // 将耗时任务 -> 异步执行 -> 提交到channel对应的NIOEventLoop的taskQueue中
        // 注意：eventLoop是一个线程，taskQueue是线程的任务队列
        ctx.channel().eventLoop().execute(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(10 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端 😵 喵", CharsetUtil.UTF_8));
                }catch(Exception e){
                    System.out.println("发生异常："+e.getMessage());
                }
            }
        });

        ctx.channel().eventLoop().execute(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(20 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端 😵 喵2", CharsetUtil.UTF_8)); // 打印会等待30秒，因为两个任务是先后进入队列的。任务队列中多个任务先后执行，是在同一个线程中执行的
                }catch(Exception e){
                    System.out.println("发生异常："+e.getMessage());
                }
            }
        });
        System.out.println("go on ...");
    }
   ```
2. 用户自定义定时任务
   ```java
    // NettyServerHandler.java中
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 如果这里有一个非常耗时的业务处理，就会一直阻塞在这里 (服务端阻塞)

        // 解决方案1：用户程序自定义的定时任务
        // 将任务提交到channel对应的NIOEventLoop的scheduleTaskQueue中
        ctx.channel().eventLoop().schedule(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(5 * 1000);
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端 😵 喵3", CharsetUtil.UTF_8));
                }catch(Exception e){
                    System.out.println("发生异常："+e.getMessage());
                }
            }
        }, 5, TimeUnit.SECONDS);
        System.out.println("go on ...");
    }
   ```
3. 非当前Reactor线程调用Channel的各种方法
   ```java
    // NettyServer.java
    serverBootstrap.group(bossGroup, workerGroup) // 设置两个线程组
        .channel(NioServerSocketChannel.class) // 使用NioServerSocketChannel作为服务器端的通道实现类（反射）
        .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列，得到连接个数
        .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
        .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道初始化对象（匿名对象）
            // 给pipeline设置处理器
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                // 在此处可以获取客户端的channel，将其保存到一个集合中管理，可以在推送消息时，将业务加入到到不同channel对应的NIOEventLoop的taskQueue或scheduleTaskQueue中执行
                System.out.println("客户socketChannel的hashcode="+socketChannel.hashCode());
                socketChannel.pipeline().addLast(new NettyServerHandler()); // 向管道最后追加一个处理器
            }
        }); // 给workerGroup的EventLoop对应的管道设置处理器
   ```

### Netty模型方案再说明
1. Netty抽象出两组线程池，BossGroup专门负责接收客户端连接，WorkerGroup专门负责网络读写操作
2. NioEventLoop表示一个不断循环执行处理任务的线程，每个NioEventLoop都有一个selector，用于监听绑定在其上的socket网络通道
3. NioEventLoop内部采用串行化设计，从消息的读取->解码->处理->编码->发送，始终由IO线程NioEventLoop负责
   1. NioEventLoopGroup下包含多个NioEventLoop
   2. 每个NioEventLoop中包含一个Selector，一个taskQueue
   3. 每个NioEventLoop的Selector上可以注册监听多个NioChannel
   4. 每个NioChannel只会绑定在唯一的NioEventLoop上
   5. 每个NioChannel都绑定有一个自己的ChannelPipeline


### Netty异步模型
1. 异步的概念与同步相对，当一个异步过程调用发出后，调用者不能立刻得到结果。实际处理这个调用的组件在完成后，通过状态，通知和回调来通知调用者。
2. Netty中的I/O操作是异步的，包括Bind，Write，Connect等操作会简单的返回一个ChannelFuture
3. 调用者并不能立刻获得结果，而是通过Future-Listener机制，用户可以方便的主动获取或者通过通知机制获得IO操作结果
4. Netty的异步模型是建立在future和callback之上的。callback就是回调，重点说future，它的核心思想是：假设一个方法run，计算过程可能非常耗时，等待run返回显然不合适，那么可以在调用run的时候，立马返回一个future，后续可以通过future去监控方法run的处理过程（即：Future-Listener机制）


### Netty实现http请求与响应（实例）
```java
/**
 * @author lixing
 * @date 2022-05-05 16:26
 * @description netty模拟http请求
 */
public class TestServer {
    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new TestServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind(7777).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
           bossGroup.shutdownGracefully();
           workerGroup.shutdownGracefully();
        }
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-05-05 16:27
 * @description
 */
public class TestServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 向管道中加入处理器
        // 得到管道
        ChannelPipeline pipeline = socketChannel.pipeline();
        // 加入一个netty提供的httpServerCodec codec => [coder - decoder]  http编解码器
        // 1. httpServerCodec 是netty提供的处理http的编解码器
        pipeline.addLast("MyHttpServerCodec", new HttpServerCodec());
        // 2. 增加一个自定义的handler
        pipeline.addLast("MyTestHttpServerHandler", new TestHttpServerHandler());
    }
}
```

```java
/**
 * @author lixing
 * @date 2022-05-05 16:26
 * @description
 */
public class TestHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    // 读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // 判断msg是不是httpRequest请求
        if(msg instanceof HttpRequest){
            HttpRequest httpRequest = (HttpRequest) msg;
            // 获取uri
            URI uri = new URI(httpRequest.uri());
            if("/favicon.ico".equals(uri.getPath())){
                System.out.println("请求了 favicon.ico，不做响应");
                return;
            }
            System.out.println("msg类型="+msg.getClass());
            System.out.println("客户端地址="+ctx.channel().remoteAddress());
            // 回复信息给浏览器 [http协议]
            ByteBuf content = Unpooled.copiedBuffer("hello，我是服务器", CharsetUtil.UTF_8);
            // 构造一个http响应，即httpResponse
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            // 将构建好的response返回
            ctx.writeAndFlush(response);
        }
    }
}
```

启动服务端后，再浏览器地址栏输入：``` http://localhost:7777  ```，访问，可以在页面显示文本 ``` hello，我是服务器 ```。但是在服务端打印可以看到，出现了两次请求
```shell
msg类型=class io.netty.handler.codec.http.DefaultHttpRequest
客户端地址=/0:0:0:0:0:0:0:1:61401
msg类型=class io.netty.handler.codec.http.DefaultHttpRequest
客户端地址=/0:0:0:0:0:0:0:1:61401
```
原因是，浏览器端发起了两次请求：
- 第一次：请求数据
- 第二次：请求网站图标文件

可以通过对URI进行判断，进行过滤

### Netty核心组件
#### Bootstrap，ServerBootstrap
1. Bootstrap是引导的意思，一个Netty应用通常由一个Bootstrap开始，主要作用是配置整个Netty程序，串联各个组件，Netty中Bootstrap类是客户端程序的启动引导类，ServerBootstrap是服务端启动引导类
2. 常用方法
   ```java
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) //用于服务端，用于设置两个EventLoopGroup
    public B group(EventLoopGroup group) //用于客户端，用来设置一个EventLoopGroup
    public B channel(Class<? extends C> channelClass) //用来设置一个服务端的通道实现
    public <T> B option(ChannelOption<T> option, T value) //用来给ServerChannel添加配置
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) //用来给接收到的通道添加配置
    public B handler(ChannelHandler handler) //【给BossGroup设置handler】
    public ServerBootstrap childHandler(ChannelHandler childHandler) //用来设置业务处理类（自定义handler）【给workerGroup设置handler】
    public ChannelFuture bind(int inetPort) //用于服务端，用来设置占用的端口号
   ```

#### Future，ChannelFuture
1. Netty中所有的IO操作都是异步的，不能立刻得知消息是否被正确处理，但是可以过一会儿等它执行完成或者直接注册一个监听，具体的实现就是通过Future和ChannelFutures，他们可以注册一个监听，当操作执行成功或失败时，监听会自动触发注册的监听事件
2. 常见的方法有：
   ```java
    Channel channel() //返回当前正在进行IO操作的通道
    ChannelFuture sync() //等待异步操作执行完毕
   ```

#### Channel
1. Netty网络通信的组件，能够用于执行网络I/O操作
2. 通过Channel可以获得当前网络连接的通道状态
3. 通过Channel可以获得网络连接的配置参数（例如接收缓冲区大小）
4. Channel提供异步的网络I/O操作（如：建立连接，读写，绑定端口），异步调用意味着任何I/O调用都将立即返回，并且不保证在调用结束时所请求的I/O操作已完成
5. 调用立即返回一个ChannelFuture实例，通过注册监听器到ChannelFuture上，可以在I/O操作成功，失败或取消时回调通知调用方
6. 支持关联I/O操作与对应的处理程序
7. 不同协议，不同阻塞类型连接都有不同的Channel类型与之对应，常用的Channel类型有：
   ```java
    NioSocketChannel //异步的客户端TCP Socket连接
    NioServerSocketChannel //异步的服务端TCP Socket连接
    NioDatagramChannel //异步的UDP连接
    NioSctpChannel //异步的客户端Sctp连接
    NioSctpServerChannel //异步的Sctp服务端连接，这些通道涵盖了UDP和TCP网络IO以及文件IO
   ```

#### Selector
1. Netty基于Selector对象实现I/O多路复用，通过Selector一个线程可以监听多个连接的Channel事件
2. 当向一个Selector中注册Channel后，Selector内部机制就可以自动不断地查询这些注册的Channel是否有已准备就绪地I/O事（例如可读，可写，网络连接完成等），这样程序就可以很简单地使用一个线程高效地管理多个Channel

#### ChannelHandler
1. ChannelHandler是一个接口，处理I/O事件或拦截I/O操作，并将其转发到其ChannelPipeline（业务处理链）中的下一个处理程序
2. ChannelHandler本身没有提供很多方法，因为这个接口有很多方法需要实现，方便使用期间可以继承它的子类
3. Channel及其实现类一览图：
   ![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0015.png)

#### Pipeline和ChannelPipeline
ChannelPipeline是一个重点：
1. ChannelPipeline是一个handler的集合，它负责处理和拦截inbound或者outbound的事件和操作，相当于一个贯穿Netty的链（也可以这样理解：ChannelPipeline是保存ChannelHandler的list，用于处理或拦截Channel的入站事件和出站事件）
2. ChannelPipeline实现了一种高级形式的拦截过滤器模式，使用户可以完全控制事件的处理方式，以及Channel中各个ChannelHandler如何相互交互
3. 在Netty中每个channel都有且仅有一个ChannelPipeline与之对应，他们的组成关系如下：
   ![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_002/0017.png)
4. 常用方法：
   ```java
   ChannelPipeline addFirst(ChannelHandler... handlers) //把一个业务处理类（handler）添加到链中的第一个位置ChannelPipeline 
   addLast(ChannelHandler... handlers) //把一个业务处理类（handler）添加到链中的最后一个位置
   ```

### ChannelHandlerContext
1. 保存Channel相关的所有上下文信息，同时关联一个ChannelHandler对象
2. 即ChannelHandlerContext中包含一个具体的事件处理器ChannelHandler，同时ChannelHandlerContext中也绑定了对应的pipeline和Channel的信息，方便对ChannelHandler进行调用
3. 常用方法：
   ```java
    ChannelFuture close() //关闭通道
    ChannelOutboundInvoker flush() //刷新
    ChannelFuture writeAndFlush(Object msg) //将数据写到ChannelPipeline中当前ChannelHandler的下一个ChannelHandler开始处理（出战）
   ```

### EventLoopGroup与其实现类NioEventLoopGroup
1. EventLoopGroup是一组EventLoop的抽象，Netty为了更好的利用多核cpu资源，一般会有多个


### Netty编解码器机制
#### 编解码器基本介绍
编解码器实现的其实就是序列化的过程（编码器=>序列化  解码器=>反序列化）

序列化的使用场景
- 对数据进行持久化（将对象数据保存到文件系统，缓存，或db中。需要对数据进行序列化）
- 在网络中传输对象数据（发送方需要先通过编码器将对象数据转换成字节流，通过socketChannel进行传输，接收端需要通过解码器将字节流还原成原始的对象数据，进行下一步的业务逻辑处理）

业务数据网络传输的过程

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0001.png)

#### Netty本身的编解码器及存在的问题
1. Netty自身提供了一些codec（编解码器）
2. 有：StringEncoder，StringDecoder，ObjectEncoder，ObjectDecoder
3. Netty本身自带的ObjectEncoder，ObjectDecoder可以用来实现POJO对象或各种业务对象的编解码，底层使用的仍是java序列化技术。而java序列化技术本身效率不高，存在如下问题：
    - 无法跨语言
    - 序列化后体积太大，是二进制编码的5倍多
    - 序列化的性能太低

#### google提出的Protobuf
1. Protobuf 是 Google 发布的开源项目，全称 Google Protocol Buffers，是一种轻便高效的结构化数据存储格式，可以用于结构化数据串行化，或者说序列化。它很适合做数据存储或 RPC [远程过程调用 remote procedure call ]数据交换格式。目前很多公司 从http + json 转向tcp + protobuf，效率会更高
2. Protobuf 是以 message 的方式来管理数据的
3. 支持跨平台、跨语言，即[客户端和服务器端可以是不同的语言编写的]（支持目前绝大多数语言，例如 C++、C#、Java、python 等）
4. 高性能，高可靠性
5. 使用 protobuf 编译器能自动生成代码，Protobuf 是将类的定义使用 .proto 文件进行描述。说明，在 idea 中编写 .proto 文件时，会自动提示是否下载 .ptoto 编写插件.可以让语法高亮。
然后通过 protoc.exe 编译器根据 .proto 自动生成 .java 文件
6. 参考文档：https://developers.google.com/protocol-buffers/docs/proto

Netty使用protobuf进行网络数据传输的流程图
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0002.png)



### Netty出站和入站机制
客户端和服务端都会有编解码器

客户端（先编码）发送数据 ——> socket通道 ——> 服务端（先解码）接收数据

其中：
- 解码decoder在InBoundHandler（入站handler）中完成的，因为服务端接收从socket通道中流过来的数据，相对于服务端来说是入站操作。而且数据传输是被编码过的字节流，所以接收后首先需要进行解码，才能进行后续的业务处理
- 编码encoder是在OutBoundHandler（出站handler）中完成的，因为客户端需要将数据发送到socket通道中，所以相对于客户端来说是出站操作。而且网络传输数据需要序列化，所以需要进行编码处理
- 而且，客户端传输数据给服务端，服务端收到后，也需要回复消息给客户端。所以，客户端，服务端的channelPipeline管道中同时包含编码encoder是在解码decoder在InBoundHandler和OutBoundHandler
- 在配置服务端serverBootStrap的childHandler，即初始化channel时，需要先向channelPipeline中添加对应的编解码器，再添加自定义的业务处理handler

![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0007.png)



### TCP粘包拆包问题及解决方案
#### TCP粘包拆包基本介绍
1. TCP是面向连接的，面向流的传输，提供高可靠性服务。收发两端都要有一一成对的socket。因此，发送端为了将多个包更有效的发送给对方，使用了优化算法（Nagle算法）：即将多次间隔较小且数据量小的数据包合并成一个大的数据块，然后进行封包。这样做虽然提高了效率，但是接收端难于分辨出完整的数据包了
2. 由于TCP无消息保护边界，需要在接收端吃力消息边界问题，也就是我们所说的粘包，拆包问题
3. 假设客户端分别发送了两个数据包 D1 和 D2 给服务端，由于服务端一次读取到字节数是不确定的，故可能存在以下四种情况：
    - 服务端分两次读取到了两个独立的数据包，分别是 D1 和 D2，没有粘包和拆包
    - 服务端一次接受到了两个数据包，D1 和 D2 粘合在一起，称之为 TCP 粘包
    - 服务端分两次读取到了数据包，第一次读取到了完整的 D1 包和 D2 包的部分内容，第二次读取到了 D2 包的剩余内容，这称之为 TCP 拆包
    - 服务端分两次读取到了数据包，第一次读取到了 D1 包的部分内容 D1_1，第二次读取到了 D1 包的剩余部分内容 D1_2 和完整的 D2 包。
    ![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0015.png)

#### TCP粘包和拆包现象示例
```java
// 服务端 MyServer.java
public class MyServer {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MyServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind(7777).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

// 服务端 MyServerInitializer.java
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyServerHandler());
    }
}

// 服务端 自定义handler MyServerHandler.java
public class MyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private int count;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        // 将buffer转成字符串
        String msg = new String(buffer, StandardCharsets.UTF_8);
        System.out.println("服务端接收数据：" + msg);
        System.out.println("服务器接收到的消息量：" + (++this.count));

        // 服务器回送数据给客户端，回送一个随机id
        ByteBuf sendMsg = Unpooled.copiedBuffer(UUID.randomUUID().toString()+" ", StandardCharsets.UTF_8);
        ctx.writeAndFlush(sendMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

```java
// 客户端 MyClient.java
public class MyClient {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new MyClientInitializer());
            ChannelFuture channelFuture = bootstrap.connect("localhost", 7777).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}

// 客户端 MyClientInitializer.java
public class MyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyClientHandler());
    }
}

// 客户端自定义handler MyClientHandler.java
public class MyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private int count;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 使用客户端发送1o条数据
        for (int i = 0; i < 10; i++) {
            ByteBuf byteBuf = Unpooled.copiedBuffer("hello,server" + i, StandardCharsets.UTF_8);
            ctx.writeAndFlush(byteBuf);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte[] buffer = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buffer);
        // 将buffer转成字符串
        String msg = new String(buffer, StandardCharsets.UTF_8);
        System.out.println("客户端接收到消息：" + msg);
        System.out.println("客户端接收到的消息量：" + (++this.count)+'\n');
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

```shell
# 分别启动三个客户端后，服务端的打印

服务端接收数据：hello,server0hello,server1hello,server2hello,server3hello,server4hello,server5hello,server6hello,server7hello,server8hello,server9
服务器接收到的消息量：1
服务端接收数据：hello,server0
服务器接收到的消息量：1
服务端接收数据：hello,server1hello,server2
服务器接收到的消息量：2
服务端接收数据：hello,server3hello,server4hello,server5
服务器接收到的消息量：3
服务端接收数据：hello,server6
服务器接收到的消息量：4
服务端接收数据：hello,server7
服务器接收到的消息量：5
服务端接收数据：hello,server8hello,server9
服务器接收到的消息量：6
服务端接收数据：hello,server0
服务器接收到的消息量：1
服务端接收数据：hello,server1
服务器接收到的消息量：2
服务端接收数据：hello,server2hello,server3hello,server4
服务器接收到的消息量：3
服务端接收数据：hello,server5hello,server6hello,server7
服务器接收到的消息量：4
服务端接收数据：hello,server8hello,server9
服务器接收到的消息量：5
```

<font color="yellow">分析打印可看出：由于传输的数据流是连续的，长度不固定。服务端无法区分不同的业务数据包。故对于客户端发送的数据包，会以拆包或粘包的形式输出</font>

#### TCP粘包和拆包的解决方案
1. 使用 自定义协议 + 编解码器，来解决
2. 关键就是要解决服务端每次读取数据长度的问题。这个问题解决了，就不会出现服务器多读或少读数据的问题，从而避免TCP的粘包，拆包。

解决方案实例：
```java
// 自定义协议包：包含属性（数据字节码内容，内容的字节长度）
public class MessageProtocol {
    private int len;
    private byte[] content;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}

// 自定义编码器
public class MyMessageEncoder extends MessageToByteEncoder<MessageProtocol> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MessageProtocol messageProtocol, ByteBuf byteBuf) throws Exception {
        System.out.println("MyMessageEncoder encode方法被调用");
        byteBuf.writeInt(messageProtocol.getLen());
        byteBuf.writeBytes(messageProtocol.getContent());
    }
}

// 自定义解码器
// ReplayingDecoder 是 byte-to-message 解码的一种特殊的抽象基类，读取缓冲区的数据之前需要检查缓冲区是否有足够的字节。
// 使用ReplayingDecoder就无需自己检查，若ByteBuf中有足够的字节，则会正常读取；若没有足够的字节则会停止解码。
public class MyMessageDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        System.out.println("MyMessageDecoder decode方法被调用");
        // 需要将得到的二进制字节码 => MessageProtocol数据包（对象）
        int len = byteBuf.readInt();
        byte[] content = new byte[len];
        byteBuf.readBytes(content);
        // 封装成MessageProtocol数据包，放入list中，传递给下一个handler进行业务处理
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(len);
        messageProtocol.setContent(content);
        list.add(messageProtocol);
    }
}
```

```java
// 服务端
public class MyServer {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MyServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind(7777).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

public class MyServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyMessageDecoder()); // 解码器：处理接收的数据
        pipeline.addLast(new MyMessageEncoder()); // 编码器：处理发送的数据
        pipeline.addLast(new MyServerHandler());
    }
}

public class MyServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    private int count;

    // 通过MessageProtocol协议包来接受通道中的数据，通过获取数据的length来精准接收单个的业务数据包，并进行消息回复等处理 
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        // 接收到数据，并处理
        int len = msg.getLen();
        byte[] content = msg.getContent();
        System.out.println("服务端接收到信息如下：");
        System.out.println("长度="+len);
        System.out.println("内容="+new String(content, StandardCharsets.UTF_8));
        System.out.println("服务器接收到消息包数量="+(++this.count));

        // 服务端回复消息给客户端（为什么客户端会收到10个回复包，因为服务器这边每收到1个数据包，就会回复一个。对于一个完整数据包的判定，就是依赖自定义的MessageProtocol）
        String responseContent = UUID.randomUUID().toString();
        int responseLen = responseContent.getBytes(StandardCharsets.UTF_8).length;
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(responseLen);
        messageProtocol.setContent(responseContent.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(messageProtocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        ctx.close();
    }
}
```

```java
// 客户端
public class MyClient {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new MyClientInitializer());
            ChannelFuture channelFuture = bootstrap.connect("localhost", 7777).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}

public class MyClientInitializer extends ChannelInitializer<SocketChannel> {
    // 由于客户端和服务端都会涉及发送和接收数据，所以需要往pipeline中同时加入编码器和解码器
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyMessageDecoder()); // 加入解码器
        pipeline.addLast(new MyMessageEncoder()); // 加入编码器
        pipeline.addLast(new MyClientHandler());
    }
}

public class MyClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {
    private int count;

    // 每次发送一次数据之前，都先用自定义的协议进行包装（业务数据长度+数据内容）
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 使用客户端发送5条数据
        for (int i = 0; i < 5; i++) {
            String msgTo = "今天天气冷，吃火锅";
            byte[] content = msgTo.getBytes(StandardCharsets.UTF_8);
            int length = msgTo.getBytes(StandardCharsets.UTF_8).length; // 获取待发送数据的长度
            // 创建协议包对象
            MessageProtocol messageProtocol = new MessageProtocol();
            messageProtocol.setContent(content);
            messageProtocol.setLen(length);
            ctx.writeAndFlush(messageProtocol);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        int len = msg.getLen();
        byte[] content = msg.getContent();

        System.out.println("客户端接收到信息如下：");
        System.out.println("长度="+len);
        System.out.println("内容="+new String(content, StandardCharsets.UTF_8));
        System.out.println("客户端接收到消息包数量="+(++this.count));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("异常消息：" + cause.getMessage());
        ctx.close();
    }
}
```

```shell
# 服务端打印

MyMessageDecoder decode方法被调用
服务端接收到信息如下：
长度=27
内容=今天天气冷，吃火锅
服务器接收到消息包数量=1
MyMessageEncoder encode方法被调用
MyMessageDecoder decode方法被调用
服务端接收到信息如下：
长度=27
内容=今天天气冷，吃火锅
服务器接收到消息包数量=2
MyMessageEncoder encode方法被调用
MyMessageDecoder decode方法被调用
服务端接收到信息如下：
长度=27
内容=今天天气冷，吃火锅
服务器接收到消息包数量=3
MyMessageEncoder encode方法被调用
MyMessageDecoder decode方法被调用
服务端接收到信息如下：
长度=27
内容=今天天气冷，吃火锅
服务器接收到消息包数量=4
MyMessageEncoder encode方法被调用
MyMessageDecoder decode方法被调用
服务端接收到信息如下：
长度=27
内容=今天天气冷，吃火锅
服务器接收到消息包数量=5
MyMessageEncoder encode方法被调用
```

```shell
# 客户端打印

MyMessageEncoder encode方法被调用
MyMessageEncoder encode方法被调用
MyMessageEncoder encode方法被调用
MyMessageEncoder encode方法被调用
MyMessageEncoder encode方法被调用
MyMessageDecoder decode方法被调用
客户端接收到信息如下：
长度=36
内容=4be633f3-fb74-49bc-8ffa-828fe9078706
客户端接收到消息包数量=1
MyMessageDecoder decode方法被调用
客户端接收到信息如下：
长度=36
内容=4242aac2-7279-400d-9704-fb5727dd9eb8
客户端接收到消息包数量=2
MyMessageDecoder decode方法被调用
客户端接收到信息如下：
长度=36
内容=30691ff2-3d2d-486d-8b92-51eb70fda47d
客户端接收到消息包数量=3
MyMessageDecoder decode方法被调用
客户端接收到信息如下：
长度=36
内容=721e6e6f-6422-4c29-955f-3a13d44352ca
客户端接收到消息包数量=4
MyMessageDecoder decode方法被调用
客户端接收到信息如下：
长度=36
内容=6bf94563-b2d4-4e22-bbb0-47a5031bab90
客户端接收到消息包数量=5
```




### Netty心跳机制
#### Netty心跳机制介绍
1. Netty提供了IdleStateHandler，ReadTimeoutHandler，WriteTimeoutHandler三个handler检测连接的有效性
2. IdleStateHandler的作用：当连接空闲时间（读或者写）太长时，会触发一个IdleStateEvent事件，然后，你可以通过ChannelInboundHandler中重写userEventTriggered方法来处理该事件。
3. }
4. ReadTimeoutHandler的作用：如果在指定时间内没有发生读事件，就会抛出异常，并自动关闭这个连接。我们可以在exceptionCaught方法中处理这个异常。
5. WriteTimeoutHandler的作用：如果在指定时间内没有发生写事件，就会抛出异常，并自动关闭这个连接。我们可以在exceptionCaught方法中处理这个异常。


### 将任务添加到异步线程池
问题：
在ServerHandler的channelRead方法中执行耗时任务时，即使使用execute()单独执行多个任务，实质上还是将任务添加到了同一个线程中。
```java
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("NettyServerHandler 线程是 = "+Thread.currentThread().getName());

        // 执行耗时任务
        ctx.channel().eventLoop().execute(()->{
            try {
                Thread.sleep(5*1000);
                System.out.println("NettyServerHandler execute1 线程是 = "+Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        ctx.channel().eventLoop().execute(()->{
            try {
                Thread.sleep(5*1000);
                System.out.println("NettyServerHandler execute2 线程是 = "+Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
```

```shell
# 打印 = 可以看出，三个线程中的线程名称是相同的，也就是说任务都添加到同一个线程中了
NettyServerHandler 线程是 = nioEventLoopGroup-3-1
NettyServerHandler execute1 线程是 = nioEventLoopGroup-3-1
NettyServerHandler execute2 线程是 = nioEventLoopGroup-3-1
```
在Netty中做耗时的，不可预料的操作，比如数据库，网络请求，会严重影响Netty对socket的处理速度。

而解决方法就是将耗时任务添加到异步线程池中，防止阻塞Netty的IO线程，可以有两种方式：
- handler中将耗时任务加入线程池
```java
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
// group就充当了业务线程池，可以将任务提交到该线程池中
static final EventExecutorGroup group = new DefaultEventExecutorGroup(16);

@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("NettyServerHandler 线程是 = "+Thread.currentThread().getName());

    // 执行耗时任务
    group.submit(()->{
        try {
            Thread.sleep(5*1000);
            System.out.println("NettyServerHandler execute1 线程是 = "+Thread.currentThread().getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

    // 执行耗时任务
    group.submit(()->{
        try {
            Thread.sleep(5*1000);
            System.out.println("NettyServerHandler execute2 线程是 = "+Thread.currentThread().getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });

    System.out.println("go on");
}
}
```
```shell
# 打印
NettyServerHandler 线程是 = nioEventLoopGroup-3-1
go on
NettyServerHandler execute2 线程是 = defaultEventExecutorGroup-4-2
NettyServerHandler execute1 线程是 = defaultEventExecutorGroup-4-1
```
- context中将耗时任务加入线程池
```java
public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        // 创建业务线程池
        EventExecutorGroup group = new DefaultEventExecutorGroup(2);
        serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class) // 使用NioServerSocketChannel作为服务器端的通道实现类（反射）
        .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列，得到连接个数
        .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
        .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道初始化对象（匿名对象）
            // 给pipeline设置处理器
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                // 在此处可以获取客户端的channel，将其保存到一个集合中管理，可以在推送消息时，将业务加入到到不同channel对应的NIOEventLoop的taskQueue或scheduleTaskQueue中执行
                System.out.println("客户socketChannel的hashcode="+socketChannel.hashCode());
                // 说明：如果我们在使用addLast()向pipeline中添加handler时，前面有指定EventExecutorGroup，那么该handler会优先加入到线程池中
                socketChannel.pipeline().addLast(group, new NettyServerHandler());
            }
        }); // 给workerGroup的EventLoop对应的管道设置处理器
    }
}

// 主要修改在这里
// socketChannel.pipeline().addLast(group, new NettyServerHandler());
```

#### 两种方式的对比
1. 第一种方式在handler中添加异步，可能更自由，比如如果需要访问数据库，那么我就异步，如果不需要，就不异步，异步会拖长接口响应时间。因为需要将任务放进mpscTask中。如果IO时间很短，task很多，可能一个循环下来，都没时间执行整个task，导致响应时间达不到要求。
2. 第二种方式是Netty的标准方式（即加入到队列），但是，这么做会将整个handler都交给业务线程池。不论耗时不耗时，都加入到队列里，不够灵活。
3. 各有优劣，从灵活性考虑，第一种较好


## Netty实现RPC
### RPC基本介绍
1. RPC（Remote Procedure Call）远程过程调用，是一个计算机通信协议。该协议允许运行于一台计算机的程序调用另一台计算机的子程序，而程序员无需额外的为这个交互作用编程
2. 两个或多个应用程序都分布在不同的服务器上，他们之间的调用都像是本地方法调用一样
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0021.png)
3. 常见的RPC框架有：阿里的Dubbo，Google的gRPC，go语言的rpcx，Apache的thrift，SpringCloud

### RPC调用流程图
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0023.png)

#### RPC 调用流程说明

1. 服务消费方（client）以本地调用方式调用服务
2. client stub 接收到调用后负责将方法、参数等封装成能够进行网络传输的消息体
3. client stub 将消息进行编码并发送到服务端
4. server stub 收到消息后进行解码
5. server stub 根据解码结果调用本地的服务
6. 本地服务执行并将结果返回给 server stub
7. server stub 将返回导入结果进行编码并发送至消费方
8. client stub 接收到消息并进行解码
9. 服务消费方（client）得到结果

小结：RPC 的目标就是将 2 - 8 这些步骤都封装起来，用户无需关心这些细节，可以像调用本地方法一样即可完成远程服务调用

### 通过Netty自己实现一个RPC
#### 需求说明
1. Dubbo 底层使用了 Netty 作为网络通讯框架，要求用 Netty 实现一个简单的 RPC 框架
2. 模仿 Dubbo，消费者和提供者约定接口和协议，消费者远程调用提供者的服务，提供者返回一个字符串，消费者打印提供者返回的数据。底层网络通信使用 Netty 4.1.20

#### 设计说明
1. 创建一个接口，定义抽象方法。用于消费者和提供者之间的约定。
2. 创建一个提供者，该类需要监听消费者的请求，并按照约定返回数据。
3. 创建一个消费者，该类需要透明的调用自己不存在的方法，内部需要使用 Netty 请求提供者返回数据
4. 开发的分析图
![](https://npm.elemecdn.com/youthlql@1.0.0/netty/introduction/chapter_003/0024.png)

#### 程序代码
> publicinterface 公共接口部分
```java
// HelloService.java
/**
 * @author lixing
 * @date 2022-05-16 10:42
 * @description 接口，服务提供方和服务消费方公用的部分
 */
public interface HelloService {
    String hello(String msg);
}
```

> netty部分
```java
// NettyServer.java
public class NettyServer {

    public static void startServer(String hostname, int port){
        startServer0(hostname, port);
    }

    // 编写一个方法，完成对NettyServer的初始化和启动
    private static void startServer0(String hostname, int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(hostname, port).sync();
            System.out.println("服务提供方开始提供服务~~");
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```
```java
// NettyServerHandler.java
/**
 * @author lixing
 * @date 2022-05-16 10:55
 * @description server端的自定义业务处理器
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 获取客户端发送的消息，并调用服务
        System.out.println("msg="+msg);
        // 客户端在调用服务器的服务时，我们需要定义一个协议。满足协议才能调用服务
        // 比如规定协议：每次发送消息都必须以某个字符串开头 “HelloService#hello”，即消息头部必须带“HelloService#”才能调用服务
        if(msg.toString().startsWith("HelloService#")){
            // 调用服务
            String res = new HelloServiceImpl().hello(msg.toString().substring(msg.toString().lastIndexOf("#") + 1));
            ctx.writeAndFlush(res);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
```
```java
// NettyClient.java
public class NettyClient {

    // 创建线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // 客户端处理器
    private static NettyClientHandler clientHandler;

    // 编写方法使用代理模式，获取一个代理对象
    public Object getBean(final Class<?> serviceClass, final String providerName){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {serviceClass}, (proxy, method, args)->{ // 每调用一次远程服务，此块代码就会重复执行一次
                    if(clientHandler == null){
                        initClient();
                    }
                    // 设置要发给服务器端的信息 (privoderName是协议头，args[0] 就是调用远程服务，传递的参数)
                    clientHandler.setParams(providerName+args[0]);
                    return executor.submit(clientHandler).get();
                });
    }

    // 初始化客户端
    private static void initClient() throws InterruptedException {
        clientHandler = new NettyClientHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(new StringEncoder());
                        pipeline.addLast(clientHandler);
                    }
                });
        bootstrap.connect("127.0.0.1", 7000).sync();
    }
}
```
```java
// NettyClientHandler.java
public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    private ChannelHandlerContext context; // 上下文
    private String result; // 返回的结果
    private String params; // 客户端调用方法时，传入的参数

    // 与服务器的连接创建成功后，就会被调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        context = ctx;
    }

    // 收到数据后，调用的方法
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        result = msg.toString();
        // 唤醒等待的线程
        notify();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    // 被代理对象调用，发送数据给服务器 -> wait -> 等待被唤醒（被channelRead唤醒） -> 返回结果
    @Override
    public synchronized Object call() throws Exception {
        // 客户端发送给服务端的消息
        context.writeAndFlush(params);
        // 进入等待，等待channelRead方法获取到服务器返回结果后，唤醒
        // 客户端发送调用远程服务的参数，等待调用到远程服务，并获取到返回结果，才进行后续操作，所以需要等待
        wait();
        return result;
    }

    void setParams(String params){
        this.params = params;
    }
}
```

> provider，服务提供方部分
```java
// HelloServiceImpl.java
// 服务方提供的服务，实现类
public class HelloServiceImpl implements HelloService {
    // 当有消费方调用该方法时，就返回一个结果
    @Override
    public String hello(String msg){
        System.out.println("收到客户端消息="+msg);
        if(msg != null){
            return "你好客户端，我已经收到你的消息["+msg+"]";
        }else{
            return "你好客户端，我已经收到你的消息";
        }
    }
}
```
```java
// ServerBootStrap.java
/**
 * @author lixing
 * @date 2022-05-16 10:47
 * @description ServerBootStrap会启动一个服务提供者，NettyServer
 */
public class ServerBootStrap {
    public static void main(String[] args) {
        NettyServer.startServer("127.0.0.1", 7000);
    }
}
```

> consumer，服务消费方部分
```java
// ClientBootStrap.java
public class ClientBootStrap {
    // 定义协议头
    public static final String providerName = "HelloService#hello#";

    public static void main(String[] args) {
        // 创建一个消费者
        NettyClient consumer = new NettyClient();
        // 创建代理对象
        HelloService service = (HelloService) consumer.getBean(HelloService.class, providerName);
        // 通过代理对象调用远程服务
        String res = service.hello("你好 dubbo~~");
        System.out.println("客户端调用远程服务的结果="+res);
    }
}
```

依次启动ServerBootStrap.java，ClientBootStrap.java。可以看到程序打印：
```shell
# 服务端
服务提供方开始提供服务~~
msg=HelloService#hello#你好 dubbo~~
收到客户端消息=你好 dubbo~~
```
```shell
# 客户端
客户端调用远程服务的结果=你好客户端，我已经收到你的消息[你好 dubbo~~]
```

---
说明：

学习视频地址：https://www.bilibili.com/video/BV1DJ411m7NR?p=1

自己整理的源码地址：https://github.com/paopaolx/Netty-learn/tree/main/netty

笔记文档部分内容参考：https://blog.csdn.net/youth_lql/category_10959696.html
