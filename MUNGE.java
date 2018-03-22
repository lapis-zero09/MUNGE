import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import py4j.GatewayServer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.DenseInstance;
import weka.core.converters.ArffSaver;
import weka.core.neighboursearch.LinearNNSearch;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("unchecked")
public class MUNGE {
    //the number of intances in the data
    protected static int m_numInst;
    protected static int m_numAttr;

    /**
     * get the matrix from Python
     * @return Instances
     * @throws Exception
    */
    public Instances createFromPy4j(byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(data);
        m_numInst = buf.getInt();
        m_numAttr = buf.getInt();
        FastVector attributes = new FastVector();
        for (int i = 0; i < m_numAttr; ++i)
            attributes.addElement(new Attribute(Integer.toString(i)));

        Instances m_data = new Instances("da", attributes, 0);
        for (int i = 0; i < m_numInst; ++i) {
            Instance instance = new DenseInstance(m_numAttr);
            for (int j = 0; j < m_numAttr; ++j)
                instance.setValue(j, buf.getDouble());
            m_data.add(instance);
        }
        return m_data;
    }

    public Instance nearestNeighbourSearch(Instances t_, Instance e) {
        Instance e_ = new DenseInstance(1);
        LinearNNSearch nns = new LinearNNSearch();
        try{
            nns.setInstances(t_);
            e_ = nns.nearestNeighbour(e);
        } catch(Exception ex) {
            System.err.println(ex.getMessage());
        }
        return e_;
    }

    /**
     * make new points
     * @return Instances
     * @throws Exception
     */
    public Instances makeNewPoints(Instances t, double p, double s, int[] l, int seed) {
        Random rand = new Random(seed);
        Random binomial = new Random(seed+1);
        Instances t_ = new Instances(t);
        for (int i = 0; i < m_numInst; i++) {
            Instance e = t_.instance(i);
            Instance e_ = nearestNeighbourSearch(t_, e);
            for (int j = 0; j < m_numAttr; j++) {
                // p
                if (binomial.nextFloat() < p) {
                    double e_j = e.value(j);
                    double e_j_ = e_.value(j);
                    if (l[j] == 0) {
                        // continuous
                        double sd = Math.abs(e_j - e_j_) / s;
                        e.setValue(j, e_j_+rand.nextGaussian()*sd);
                        e_.setValue(j, e_j+rand.nextGaussian()*sd);
                    } else {
                        // not continuous
                        e.setValue(j, e_j_);
                        e_.setValue(j, e_j);
                    }
                }
            }
        }
        return t_;
    }

    /**
     * MUNGE
     * @return munge filename
     * @throws Exception
     */
    public String munge(byte[] data) throws Exception {
        Instances t = createFromPy4j(data);
        Instances d = new Instances(t);
        // represent instances attributes for munge
        // 0 : continuous
        // 1 : other
        int[] l = new int[m_numAttr];
        Arrays.fill(l, 0);
        // for (int i = 0; i < 10; i++)
        //     l[i] = 0;

        // parameter
        double p = 0.5;
        double s = 10;
        int k = 1000000;
        int iter = (int)(k/t.numInstances()+1);

        // for Stream
        List<Instances> job = new ArrayList();
        FastVector attributes = new FastVector();
        attributes.addElement(new Attribute(Integer.toString(0)));
        Random seedRand = new Random(123);
        for (int i = 0; i < iter; i++) {
            Instances insts = new Instances("", attributes, 0);
            Instance instance = new DenseInstance(1);
            instance.setValue(0, seedRand.nextInt(100));
            insts.add(instance);
            job.add(insts);
        }
        attributes = null;
        seedRand = null;
        Stream<Instances> parallelStream = job.parallelStream();

        parallelStream.map(x -> makeNewPoints(t,p,s,l,(int)x.instance(0).value(0)))
            .collect(Collectors.toList())
            .forEach(x -> {
                for (int i = 0; i < x.numInstances(); ++i) {
                    d.add(x.instance(i));
                }
            });

        String filename = "./data/munge.arff";
        ArffSaver saver = new ArffSaver();
        saver.setInstances(d);
        saver.setFile(new File(filename));
        saver.writeBatch();
        return filename;
    }

    /**
     * connect to Python
     * @return
     * @throws Exception
    */
    public static void main(String[] args) throws Exception {
        MUNGE app = new MUNGE();
        GatewayServer server = new GatewayServer(app);
        server.start();
        System.out.println("  [!] py4j Gateway Server Started");
    }
}
