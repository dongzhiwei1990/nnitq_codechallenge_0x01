
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FileCounter2 {
    //待遍历的文件夹
    private static Map<Integer, File> dirMap = new ConcurrentHashMap<>();
    //队列当前最大下标
    private static AtomicInteger linkedIndex = new AtomicInteger(0);
    //当前遍历下标
    private static AtomicInteger loopIndex = new AtomicInteger(0);
    //空文件夹数量
    private static AtomicInteger emptyDirCount = new AtomicInteger(0);
    //总文件数量
    private static AtomicInteger fileCount = new AtomicInteger(0);
    //遍历完成
    private static volatile Boolean complete = false;

    public static void main(String[] args) throws Exception {
        //TODO 设置要遍历的目录
        String path = "C:\\Windows";
        
        
        System.out.println("开始读取：" + path);
        long st = System.currentTimeMillis();
        directoryTraversal(path);
        long et = System.currentTimeMillis();
        System.out.println("CPU :i7-8550U_CPU_@_1.80GHz 2.00GHz");
        System.out.println("MEM :16G");
        System.out.println("OS :win10");
        System.out.println("代码地址 :");
        System.out.println("程序执行时间 :" + (BigDecimal.valueOf(et - st).divide(BigDecimal.valueOf(1000)).setScale(3)));
        System.out.println("所有文件总数 :" + fileCount.get());
        System.out.println("空文件夹个数 :" + emptyDirCount.get());


    }

    private static void addDir(File file) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("file", file);
        //读取中标识
        fileInfo.put("reading", new AtomicInteger(0));
        //读取完成标识
        //fileInfo.put("finished", new AtomicInteger(0));
        dirMap.put(linkedIndex.getAndIncrement(), file);
    }

    private static void directoryTraversal(String path) throws Exception {
        if (!Files.isDirectory(Paths.get(path))) {
            throw new Exception("无效的目录路径：" + path);
        }
        //设置初始遍历根目录
        addDir(Paths.get(path).toFile());
        //线程数=CPU核数；本机电脑8核
        final int threads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < threads; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int fc = 0;
                    while (true) {
                        if (complete) {
                            return;
                        }
                        long st = System.currentTimeMillis();
                        int ik = loopIndex.get();
                        if (!dirMap.containsKey(ik)) {
                            continue;
                        }
                        //自旋获取遍历下标
                        if (!loopIndex.compareAndSet(ik, ik + 1)) {
                            continue;
                        }
                        File dir = dirMap.get(ik);
                        if (null == dir) {
                            continue;
                        }
                        if (Files.isSymbolicLink(dir.toPath())) {
                            continue;
                        }
                        File[] fileList = dir.listFiles();
                        if (null == fileList || fileList.length < 1) {
                            emptyDirCount.getAndIncrement();
                            dirMap.remove(ik);
                            continue;
                        }
                        fc = 0;
                        Map<Integer, File> newMap = new HashMap<>();
                        for (File f : fileList) {
                            if (f.isDirectory()) {
                                newMap.put(linkedIndex.getAndIncrement(), f);
                                continue;
                            }
                            ++fc;
                        }
                        dirMap.putAll(newMap);
                        if (fc > 0) {
                            fileCount.getAndAdd(fc);
                        }
                        dirMap.remove(ik);
                        long t = System.currentTimeMillis() - st;
                        if (t > 666) {
                            System.out.println("单次高耗时 :" + (t) + "ms   " + dir.toPath() + "  文件/文件夹数量=" + fileList.length);
                        }

                    }
                }
            }).start();
        }
        long st = System.currentTimeMillis();
        while (true) {
            Thread.sleep(256);
            System.out.println("当前已遍历（"
                    + (BigDecimal.valueOf(System.currentTimeMillis() - st).divide(BigDecimal.valueOf(1000)).setScale(3))
                    + "）：总文件数=" + fileCount + "；空文件夹数="
                    + emptyDirCount + "；缓存队列大小=" + dirMap.size());


            if (dirMap.isEmpty()) {
                complete = true;
                break;
            }
        }
    }


}
