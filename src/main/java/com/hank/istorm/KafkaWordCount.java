package com.hank.istorm;

        import org.apache.commons.lang.StringUtils;
        import org.apache.storm.Config;
        import org.apache.storm.LocalCluster;
        import org.apache.storm.generated.StormTopology;
        import org.apache.storm.kafka.spout.KafkaSpout;
        import org.apache.storm.kafka.spout.KafkaSpoutConfig;
        import org.apache.storm.topology.BasicOutputCollector;
        import org.apache.storm.topology.OutputFieldsDeclarer;
        import org.apache.storm.topology.TopologyBuilder;
        import org.apache.storm.topology.base.BaseBasicBolt;
        import org.apache.storm.tuple.Fields;
        import org.apache.storm.tuple.Tuple;
        import org.apache.storm.tuple.Values;
        import java.util.Map;
        import java.util.concurrent.ConcurrentHashMap;

public class KafkaWordCount {
    /**
     * 上海(Bolt) Reduce
     */
    public static class KafakaConsumerBolt extends BaseBasicBolt{
        @Override
        public void execute(Tuple input, BasicOutputCollector collector) {
            String value = input.getStringByField("value");
            System.err.println(value);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

        }
    }

    public static void main(String[] args) {
        //topology
        TopologyBuilder builder = new TopologyBuilder();

        //由上到下处理
        //雪山
        builder.setSpout("kafkaSpout",
                new KafkaSpout<>(
                        KafkaSpoutConfig
                                .builder("192.168.85.211:9092", "xmiss")
                                //默认拉数据是第一个开始(最后的数据)
                                .setFirstPollOffsetStrategy(KafkaSpoutConfig.FirstPollOffsetStrategy.LATEST)
                                .build()), 1);

        //上海
        builder.setBolt("kafkaConsumerBolt",new KafakaConsumerBolt()).shuffleGrouping("kafkaSpout");
        StormTopology topology = builder.createTopology();

        //提交到线上集群
        //StormSubmitter.submitTopology();
        //提交到本地运行
        new LocalCluster().submitTopology("wordcount",new Config(),topology);
    }
}

