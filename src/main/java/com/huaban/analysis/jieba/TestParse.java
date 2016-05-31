package com.huaban.analysis.jieba;

/**
 * Created by zfy on 2016/5/25.
 */
public class TestParse {
    public static void main(String[] args) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String[] sentences =
                new String[] {"这是一个伸手不见五指的黑夜。我叫张枫旸，我爱北京邮电大学，我爱Java和Scala"};
        for (String sentence : sentences) {
            System.out.println(segmenter.process(sentence, JiebaSegmenter.SegMode.INDEX).toString());
        }
    }
}
