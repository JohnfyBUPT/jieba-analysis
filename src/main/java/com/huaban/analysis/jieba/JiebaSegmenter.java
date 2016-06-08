package com.huaban.analysis.jieba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.huaban.analysis.jieba.viterbi.FinalSeg;


public class JiebaSegmenter {
    private static WordDictionary wordDict = WordDictionary.getInstance();
    private static FinalSeg finalSeg = FinalSeg.getInstance();

    public static enum SegMode {
        INDEX,
        SEARCH
    }

    //获得有向无环图
    private Map<Integer, List<Integer>> createDAG(String sentence) {
        Map<Integer, List<Integer>> dag = new HashMap<>();
        DictSegment trie = wordDict.getTrie();
        char[] chars = sentence.toCharArray();
        int N = chars.length;
        int i = 0, j = 0;
        while (i < N) {
            Hit hit = trie.match(chars, i, j - i + 1);
            if (hit.isPrefix() || hit.isMatch()) {
                if (hit.isMatch()) {
                    if (!dag.containsKey(i)) {
                        List<Integer> value = new ArrayList<>();
                        dag.put(i, value);
                        value.add(j);
                    }
                    else
                        dag.get(i).add(j);
                }
                //如果只是前缀而没有完全匹配，则词长度向后加一
                //如果匹配到，则词长度向后加一，碰到结尾则重新初始化
                j += 1;
                if (j >= N) {
                    i += 1;
                    j = i;
                }
            }
            else {
                i += 1;
                j = i;
            }
        }
        //把未被匹配的单字加入有向无环图
        for (i = 0; i < N; ++i) {
            if (!dag.containsKey(i)) {
                List<Integer> value = new ArrayList<>();
                value.add(i);
                dag.put(i, value);
            }
        }
        return dag;
    }


    private Map<Integer, Pair<Integer>> calc(String sentence, Map<Integer, List<Integer>> dag) {
        int N = sentence.length();
        HashMap<Integer, Pair<Integer>> route = new HashMap<>();
        route.put(N, new Pair<>(0, 0.0));
        // 找出每个字为词首，最可能的组词形式，并记录下组词的结尾以及频率因数
        for (int i = N - 1; i > -1; i--) {
            Pair<Integer> candidate = null;
            for (Integer x : dag.get(i)) {
                double freq = wordDict.getFreq(sentence.substring(i, x + 1)) + route.get(x + 1).freq;
                if (candidate == null) {
                    candidate = new Pair<>(x, freq);
                }
                else if (candidate.freq < freq) {
                    candidate.freq = freq;
                    candidate.key = x;
                }
            }
            route.put(i, candidate);
        }
        return route;
    }


    public List<SegToken> process(String paragraph, SegMode mode) {
        List<SegToken> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < paragraph.length(); ++i) {
            char ch = CharacterUtil.regularize(paragraph.charAt(i));
            //如果找到的是中文字符且不是最后一个，都加入处理语块中
            if (CharacterUtil.ccFind(ch) && i!= paragraph.length()-1 )
                sb.append(ch);
            //遇到标点符号或尾部，开始处理语块
            else {
                if (sb.length() > 0) {
                    //开始处理
                    //SEARCH模式下，只处理一次句子，不对长的词句再次分解
                    if (mode == SegMode.SEARCH) {
                        for (String word : sentenceProcess(sb.toString())) {
                            tokens.add(new SegToken(word, offset, offset += word.length()));
                        }
                    }
                    //INDEX模式下，对长的词句不仅将其自身加入token，并且将其中的长度为2和3的词也加入token中
                    if (mode == SegMode.INDEX) {
                        for (String token : sentenceProcess(sb.toString())) {
                            if (token.length() > 2) {
                                String gram2;
                                int j = 0;
                                for (; j < token.length() - 1; ++j) {
                                    gram2 = token.substring(j, j + 2);
                                    if (wordDict.containsWord(gram2))
                                        tokens.add(new SegToken(gram2, offset + j, offset + j + 2));
                                }
                            }
                            if (token.length() > 3) {
                                String gram3;
                                int j = 0;
                                for (; j < token.length() - 2; ++j) {
                                    gram3 = token.substring(j, j + 3);
                                    if (wordDict.containsWord(gram3))
                                        tokens.add(new SegToken(gram3, offset + j, offset + j + 3));
                                }
                            }
                            tokens.add(new SegToken(token, offset, offset += token.length()));
                        }
                    }
                    sb = new StringBuilder();
                    offset = i;
                }
                //将标点符号也加入token中
                if (wordDict.containsWord(paragraph.substring(i, i + 1))) {
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
                }
                else {
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
                }
            }
        }
        return tokens;
    }


    /*
     *
     */
    public List<String> sentenceProcess(String sentence) {
        List<String> tokens = new ArrayList<>();
        //得到有向无环图
        Map<Integer, List<Integer>> dag = createDAG(sentence);
        //得到句子的路径
        Map<Integer, Pair<Integer>> route = calc(sentence, dag);
        int x = 0;
        int y = 0;
        String buf;
        StringBuilder sb = new StringBuilder();
        while (x < sentence.length()) {
            y = route.get(x).key + 1;
            String lWord = sentence.substring(x, y);
            //单个字组成词的情况，先存入buffer
            if (y - x == 1) {
                sb.append(lWord);
            }
            //多个字组成词的情况，把该词加入tokens，并处理一下buffer,同时buffer清空
            else {
                if (sb.length() > 0) {
                    buf = sb.toString();
                    sb = new StringBuilder();
                    //buffer里只有一个单字
                    if (buf.length() == 1) {
                        tokens.add(buf);
                    }
                    //buffer里有多个连续单字
                    else {
                        if (wordDict.containsWord(buf)) {
                            tokens.add(buf);
                        }
                        else {
                            finalSeg.cut(buf, tokens);
                        }
                    }
                }
                tokens.add(lWord);
            }
            x = y;
        }
        buf = sb.toString();
        //处理最后一次的buffer
        if (buf.length() > 0) {
            if (buf.length() == 1) {
                tokens.add(buf);
            }
            else {
                if (wordDict.containsWord(buf)) {
                    tokens.add(buf);
                }
                else {
                    finalSeg.cut(buf, tokens);
                }
            }

        }
        return tokens;
    }
}
