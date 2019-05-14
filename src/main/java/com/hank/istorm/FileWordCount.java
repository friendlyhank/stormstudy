package com.hank.istorm;

import org.apache.commons.lang.StringUtils;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileWordCount {
    /**
     * 雪山(Spout)
     */
    public static class FileSpout extends BaseRichSpout{
        private String filename = "d:/logtest/a.txt";
        private BufferedReader br = null;
        private SpoutOutputCollector colletcor;
        /**
         * 整个job只会执行一次,开一次水就好
         * @param conf
         * @param topologyContext
         * @param colletcor
         */
        @Override
        public void open(Map conf, TopologyContext topologyContext, SpoutOutputCollector colletcor) {
            try {
                this.br = new BufferedReader(new FileReader(this.filename));
                this.colletcor =colletcor;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        /**
         * 有水来了，就去处理，并把结果往后送
         */
        @Override
        public void nextTuple() {
            try {
                String line = br.readLine();
                if(StringUtils.isBlank(line)){
                    return;
                }

                this.colletcor.emit(new Values(line));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 送出去的数据的name,类似form表达的name
         * @param declarer
         */
        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("line"));
        }
    }

    /**
     * 武汉(Bolt) Map
     */
    public static class MapBolt extends BaseBasicBolt{
        @Override
        public void execute(Tuple input, BasicOutputCollector collector) {
            String line = input.getStringByField("line");
            if(StringUtils.isBlank(line)){
                return;
            }
            String[] words = line.split(" ");
            for(String word : words){
                collector.emit(new Values(word,1));
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("word","count"));
        }
    }

    /**
     * 上海(Bolt) Reduce
     */
    public static class ReduceBolt extends BaseBasicBolt{
        private static Map<String,Integer> wordCountMap = new ConcurrentHashMap<>();

        @Override
        public void execute(Tuple input, BasicOutputCollector collector) {
            String word = input.getStringByField("word");
            Integer count = input.getIntegerByField("count");
            Integer currentCount = wordCountMap.get(word);
            if(null == currentCount){
                currentCount =0;
            }
            currentCount = currentCount + count;
            wordCountMap.put(word,currentCount);

            System.err.println(wordCountMap);
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
        builder.setSpout("fileSpout",new FileSpout());

        //武汉
        builder.setBolt("mapBolt",new MapBolt()).shuffleGrouping("fileSpout");

        //上海
        builder.setBolt("reduceBolt",new ReduceBolt()).shuffleGrouping("mapBolt");
        StormTopology topology = builder.createTopology();

        //提交到线上集群
        //StormSubmitter.submitTopology();
        //提交到本地运行
        new LocalCluster().submitTopology("wordcount",new Config(),topology);
    }
}
