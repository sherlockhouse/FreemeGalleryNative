package com.freeme.camera.mode.ai;

import java.util.List;

public class IKOBean {


    /**
     * log_id : 6505382230756200693
     * result_num : 5
     * result : [{"score":0.962553,"root":"商品-容器","baike_info":{"baike_url":"http://baike.baidu.com/item/%E7%8E%BB%E7%92%83%E6%9D%AF/22382","image_url":"http://imgsrc.baidu.com/baike/pic/item/5ab5c9ea15ce36d3641728a030f33a87e950b10b.jpg","description":"玻璃杯(glass)是指玻璃制成的杯子，通常由原材料高硼硅玻璃，经过600多度的高温烧制而成，它是新型的环保型茶杯，越来越受到人们的青睐。"},"keyword":"玻璃杯"},{"score":0.635507,"root":"商品-容器","baike_info":{"baike_url":"http://baike.baidu.com/item/%E6%9D%AF%E5%AD%90/70511","image_url":"http://imgsrc.baidu.com/baike/pic/item/4bed2e738bd4b31c6f6280458ed6277f9f2ff8f6.jpg","description":"杯子，(一种专门盛水的器皿)从古至今其主要功能都是用来饮酒或饮茶。基本器型大多是直口或敞口，口沿直径与杯高近乎相等。有平底、圈足或高足。考古资料表明最早的杯始见于新石器时代。无论是在仰韶文化、龙山文化还是河姆渡文化遗址中都见有陶制杯的存在，这一时期杯型最为奇特多样：带耳的有单耳或双耳杯、带足的多为锥形、三足杯、觚形杯、高柄杯等等，根据制作材料不同，可以分为玻璃杯、塑料杯、陶瓷杯、木杯等。"},"keyword":"杯子"},{"score":0.435788,"root":"商品-容器","baike_info":{"baike_url":"http://baike.baidu.com/item/%E6%B0%B4%E5%A3%B6/7064668","image_url":"http://imgsrc.baidu.com/baike/pic/item/bd3eb13533fa828bedfbcd9dfd1f4134960a5af7.jpg","description":"水壶，是一种盛水的容器。有很多种材质，可以指烧水用的金属壶，也可以指便于携带的饮用水壶，主要分为五大类：1、塑料的(主要材料)2、不锈钢的3、铝合金的4、陶瓷的5、其它材质的，可由电加热也可直接用火加热。"},"keyword":"水壶"},{"score":0.214299,"root":"商品-容器","baike_info":{"baike_url":"http://baike.baidu.com/item/%E6%B0%B4%E6%9D%AF/5488778","image_url":"http://imgsrc.baidu.com/baike/pic/item/4610b912c8fcc3ce270e272c9945d688d53f20e7.jpg","description":"水杯汉语拼音是 shuǐ bēi，英语名是 cup 。水杯通常是人们盛装液体的容器，平时可用来喝茶、喝水、喝咖啡、喝饮料等。水杯是一种大多数情况下用来盛载液体的器皿。通常用塑胶、玻璃、瓷,不锈钢制造，在餐厅打包饮料，则常用纸杯或胶杯盛载。杯子多呈圆柱形，上面开口，中空，以供盛物。因杯开口，杯内液体易被四周尘埃污染，所以当长时间放置，多用杯盖遮掩。盛载热饮的杯有手柄，这样方便使用。在各国的不同领域和文化中，杯子的形状有个不一样，可以说文化决定形状。水杯也有很多种类，如保温杯，开口杯，环行杯，智能水杯等等"},"keyword":"水杯"},{"score":0.006482,"root":"商品-容器","baike_info":{},"keyword":"玻璃壶"}]
     */

    private long log_id;
    private int result_num;
    private List<ResultBean> result;

    public long getLog_id() {
        return log_id;
    }

    public void setLog_id(long log_id) {
        this.log_id = log_id;
    }

    public int getResult_num() {
        return result_num;
    }

    public void setResult_num(int result_num) {
        this.result_num = result_num;
    }

    public List<ResultBean> getResult() {
        return result;
    }

    public void setResult(List<ResultBean> result) {
        this.result = result;
    }

    public static class ResultBean {
        /**
         * score : 0.962553
         * root : 商品-容器
         * baike_info : {"baike_url":"http://baike.baidu.com/item/%E7%8E%BB%E7%92%83%E6%9D%AF/22382","image_url":"http://imgsrc.baidu.com/baike/pic/item/5ab5c9ea15ce36d3641728a030f33a87e950b10b.jpg","description":"玻璃杯(glass)是指玻璃制成的杯子，通常由原材料高硼硅玻璃，经过600多度的高温烧制而成，它是新型的环保型茶杯，越来越受到人们的青睐。"}
         * keyword : 玻璃杯
         */

        private double score;
        private String root;
        private BaikeInfoBean baike_info;
        private String keyword;

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public BaikeInfoBean getBaike_info() {
            return baike_info;
        }

        public void setBaike_info(BaikeInfoBean baike_info) {
            this.baike_info = baike_info;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public static class BaikeInfoBean {
            /**
             * baike_url : http://baike.baidu.com/item/%E7%8E%BB%E7%92%83%E6%9D%AF/22382
             * image_url : http://imgsrc.baidu.com/baike/pic/item/5ab5c9ea15ce36d3641728a030f33a87e950b10b.jpg
             * description : 玻璃杯(glass)是指玻璃制成的杯子，通常由原材料高硼硅玻璃，经过600多度的高温烧制而成，它是新型的环保型茶杯，越来越受到人们的青睐。
             */

            private String baike_url;
            private String image_url;
            private String description;

            public String getBaike_url() {
                return baike_url;
            }

            public void setBaike_url(String baike_url) {
                this.baike_url = baike_url;
            }

            public String getImage_url() {
                return image_url;
            }

            public void setImage_url(String image_url) {
                this.image_url = image_url;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }
    }
}
