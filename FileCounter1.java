
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FileCounter1 {
    //待遍历的文件夹{path:{file:File,reading:AtomicInteger}}
    private static Map<String, Map<String, Object>> dirMap = new ConcurrentHashMap<>();
    private static Map<String, Map<String, Object>> readingDirMap = new ConcurrentHashMap<>();
    //空文件夹数量
    private static AtomicInteger emptyDirCount=new AtomicInteger(0);
    //总文件数量
    private static AtomicInteger fileCount=new AtomicInteger(0);
    //遍历完成
    private static volatile Boolean complete = false;

    public static void main(String[] args) throws Exception {
        //TODO 设置要遍历的目录
        String path = "C:\\Windows";
        
        
        System.out.println("开始读取："+path);
        long st = System.currentTimeMillis();
        directoryTraversal(path);
        long et = System.currentTimeMillis();
        System.out.println("微信昵称 :余笙");
        System.out.println("CPU :i7-8550U_CPU_@_1.80GHz 2.00GHz");
        System.out.println("MEM :16G");
        System.out.println("OS :win10");
        System.out.println("代码地址 :https://github.com/dongzhiwei1990/nnitq_codechallenge_0x01.git");
        System.out.println("程序执行时间 :"+(BigDecimal.valueOf(et-st).divide(BigDecimal.valueOf(1000)).setScale(3)));
        System.out.println("所有文件总数 :"+fileCount.get());
        System.out.println("空文件夹个数 :"+emptyDirCount.get());





    }

    private static void addDir(File file) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("file", file);
        //读取中标识
        fileInfo.put("reading", new AtomicInteger(0));
        dirMap.put(file.getAbsolutePath(), fileInfo);
    }

    private static void directoryTraversal(String path) throws Exception {
        if (!Files.isDirectory(Paths.get(path))) {
            throw new Exception("无效的目录路径：" + path);
        }
        //设置初始遍历根目录
        addDir(Paths.get(path).toFile());
        //线程数=CPU核数；本机电脑8核
        final int threads = 8;
        for (int i = 0; i < threads; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        long st = System.currentTimeMillis();
                        Iterator<Map<String, Object>> it = dirMap.values().iterator();
                        String ph = null;
                        int fcount = 0;
                        while (it.hasNext()) {
                            Map<String, Object> fileInfo = it.next();
                            AtomicInteger reading = (AtomicInteger) fileInfo.get("reading");
                            if (reading.get() > 0) {
                                //System.out.println(Thread.currentThread().getId()+"**"+((File) fileInfo.get("file")).getAbsolutePath());
                                continue;
                            }
                            if (reading.compareAndSet(0, 1)) {
                                File dir = (File) fileInfo.get("file");
                                ph=dir.getAbsolutePath();
                                File[] fileList = dir.listFiles();
                                if(null==fileList||fileList.length<1){
                                    emptyDirCount.getAndIncrement();
                                    dirMap.remove(dir.getAbsolutePath());
                                    break;
                                }
                                fcount = fileList.length;
                                readingDirMap.put(dir.getAbsolutePath(), fileInfo);
                                int fc = 0;
                                for (File file : fileList) {
                                    long st222 = System.currentTimeMillis();
                                    if (file.isDirectory()) {
                                        long t = System.currentTimeMillis()-st222;
                                        if(t>11){
                                            System.out.println("单次高耗时* :"+(t)+"ms   "+ph+"  文件/文件夹数量="+fcount);
                                        }
                                        addDir(file);
                                         t = System.currentTimeMillis()-st222;
                                        if(t>22){
                                            System.out.println("单次高耗时** :"+(t)+"ms   "+ph+"  文件/文件夹数量="+fcount);
                                        }
                                        continue;
                                    }
                                    ++fc;
                                }
                                fileCount.getAndAdd(fc);
                                readingDirMap.remove(dir.getAbsolutePath());
                                dirMap.remove(dir.getAbsolutePath());
                                break;
                            }
                            //AtomicInteger finished = (AtomicInteger) fileInfo.get("finished");
                        }
                        long t = System.currentTimeMillis()-st;
                        if(t>123){
                            System.out.println("单次高耗时 :"+(t)+"ms   "+ph+"  文件/文件夹数量="+fcount);
                        }
                        if(complete){
                            return;
                        }
                    }
                }
            }).start();
        }
        while (true){
            Thread.sleep(128);
            System.out.println("当前已遍历：总文件数="+fileCount
                    +"；空文件夹数="+emptyDirCount
                    +"；待读取缓存队列大小="+dirMap.size()
                    +"；读取中缓存队列大小="+readingDirMap.size());

            if(dirMap.isEmpty()&&readingDirMap.isEmpty()){
                complete=true;
                break;
            }
        }
    }


}
