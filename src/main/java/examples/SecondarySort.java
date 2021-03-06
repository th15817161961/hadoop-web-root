package examples;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SecondarySort {
    public SecondarySort() {
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new String[2];
        otherArgs[0] = "hdfs://127.0.0.1:9000/out4";
        otherArgs[1] = "hdfs://127.0.0.1:9000/out5";
        if (otherArgs.length != 2) {
            System.err.println("Usage: secondarysort <in> <out>");
            System.exit(2);
        }

        Job job = Job.getInstance(conf, "secondary sort");
        job.setJarByClass(SecondarySort.class);
        job.setMapperClass(SecondarySort.MapClass.class);
        job.setReducerClass(SecondarySort.Reduce.class);
        job.setPartitionerClass(SecondarySort.FirstPartitioner.class);
        job.setGroupingComparatorClass(SecondarySort.FirstGroupingComparator.class);
        job.setMapOutputKeyClass(SecondarySort.IntPair.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class Reduce extends Reducer<SecondarySort.IntPair, IntWritable, Text, IntWritable> {
        private static final Text SEPARATOR = new Text("------------------------------------------------");
        private final Text first = new Text();

        public Reduce() {
        }

        public void reduce(SecondarySort.IntPair key, Iterable<IntWritable> values, Reducer<SecondarySort.IntPair, IntWritable, Text, IntWritable>.Context context) throws IOException, InterruptedException {
            context.write(SEPARATOR, null);
            this.first.set(Integer.toString(key.getFirst()));
            Iterator var4 = values.iterator();

            while(var4.hasNext()) {
                IntWritable value = (IntWritable)var4.next();
                context.write(this.first, value);
            }

        }
    }

    public static class MapClass extends Mapper<LongWritable, Text, SecondarySort.IntPair, IntWritable> {
        private final SecondarySort.IntPair key = new SecondarySort.IntPair();
        private final IntWritable value = new IntWritable();

        public MapClass() {
        }

        public void map(LongWritable inKey, Text inValue, Mapper<LongWritable, Text, SecondarySort.IntPair, IntWritable>.Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(inValue.toString());
            int left = 0;
            int right = 0;
            if (itr.hasMoreTokens()) {
                left = itr.countTokens();
                if (itr.hasMoreTokens()) {
                    right = itr.countTokens();
                }

                this.key.set(left, right);
                this.value.set(right);
                context.write(this.key, this.value);
            }

        }
    }

    public static class FirstGroupingComparator implements RawComparator<SecondarySort.IntPair> {
        public FirstGroupingComparator() {
        }

        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return WritableComparator.compareBytes(b1, s1, 4, b2, s2, 4);
        }

        public int compare(SecondarySort.IntPair o1, SecondarySort.IntPair o2) {
            int l = o1.getFirst();
            int r = o2.getFirst();
            return l == r ? 0 : (l < r ? -1 : 1);
        }
    }

    public static class FirstPartitioner extends Partitioner<SecondarySort.IntPair, IntWritable> {
        public FirstPartitioner() {
        }

        public int getPartition(SecondarySort.IntPair key, IntWritable value, int numPartitions) {
            return Math.abs(key.getFirst() * 127) % numPartitions;
        }
    }

    public static class IntPair implements WritableComparable<SecondarySort.IntPair> {
        private int first = 0;
        private int second = 0;

        public IntPair() {
        }

        public void set(int left, int right) {
            this.first = left;
            this.second = right;
        }

        public int getFirst() {
            return this.first;
        }

        public int getSecond() {
            return this.second;
        }

        public void readFields(DataInput in) throws IOException {
            this.first = in.readInt() + -2147483648;
            this.second = in.readInt() + -2147483648;
        }

        public void write(DataOutput out) throws IOException {
            out.writeInt(this.first - -2147483648);
            out.writeInt(this.second - -2147483648);
        }

        public int hashCode() {
            return this.first * 157 + this.second;
        }

        public boolean equals(Object right) {
            if (!(right instanceof SecondarySort.IntPair)) {
                return false;
            } else {
                SecondarySort.IntPair r = (SecondarySort.IntPair)right;
                return r.first == this.first && r.second == this.second;
            }
        }

        public int compareTo(SecondarySort.IntPair o) {
            if (this.first != o.first) {
                return this.first < o.first ? -1 : 1;
            } else if (this.second != o.second) {
                return this.second < o.second ? -1 : 1;
            } else {
                return 0;
            }
        }

        static {
            WritableComparator.define(SecondarySort.IntPair.class, new SecondarySort.IntPair.Comparator());
        }

        public static class Comparator extends WritableComparator {
            public Comparator() {
                super(SecondarySort.IntPair.class);
            }

            public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
                return compareBytes(b1, s1, l1, b2, s2, l2);
            }
        }
    }
}
