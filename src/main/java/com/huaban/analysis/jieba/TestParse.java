package com.huaban.analysis.jieba;

/**
 * Created by zfy on 2016/5/25.
 */
public class TestParse {
    public static void main(String[] args) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String[] sentences =
                new String[] {"第五个Map，emit，即发射概率矩阵。是在某个状态观察到某个观察值（即某个字）的概率。由以下代码得到。其中PROB_EMIT文件存储了几千个字和他们在对应状态时出现的概率。"};
        //我叫张枫旸，我爱北京邮电大学，我爱Java和Scala
        for (String sentence : sentences) {
            System.out.println(segmenter.process(sentence, JiebaSegmenter.SegMode.INDEX).toString());
        }
    }
}
