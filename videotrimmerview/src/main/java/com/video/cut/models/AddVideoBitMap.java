package com.video.cut.models;

import java.util.List;

/**
 * Created by Administrator on 2018/8/9.
 */

public class AddVideoBitMap {


    private List<MainAssetsBean> mainAssets;

    public List<MainAssetsBean> getMainAssets() {
        return mainAssets;
    }

    public void setMainAssets(List<MainAssetsBean> mainAssets) {
        this.mainAssets = mainAssets;
    }

    public static class MainAssetsBean {
        /**
         * ct : 1403
         * id : 1
         * start : 0
         * duration : 7
         * w : 960
         * h : 540
         * an : 90
         * url : http://itbour-user.oss-cn-hangzhou.aliyuncs.com/video/U31/2018/06/166/132717791_PedyVYFNN8YEbcMFwf6J
         * coverImage : http://itbour-user.oss-cn-hangzhou.aliyuncs.com/video/U20/2018/06/157/164335966_MfTwbt66GsZzOUI6IdHA
         * processedUrl : http://itbour-user.oss-cn-hangzhou.aliyuncs.com/video/U31/2018/06/166/132717791_PedyVYFNN8YEbcMFwf6J
         * subTitleRatio : 1.3
         * subTitles : [{"id":"11233","te":"123","startTime":1,"duration":3,"fn":"微软雅黑","fs":17,"color":{"r":200,"g":200,"b":200,"a":1},"pos":{"x":0,"y":0,"w":100,"h":100,"an":0}}]
         */

        private int ct;
        private int id;
        private int start;
        private int duration;
        private int w;
        private int h;
        private int an;
        private String url;
        private String coverImage;
        private String processedUrl;
        private double subTitleRatio;
        private List<SubTitlesBean> subTitles;

        public int getCt() {
            return ct;
        }

        public void setCt(int ct) {
            this.ct = ct;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getW() {
            return w;
        }

        public void setW(int w) {
            this.w = w;
        }

        public int getH() {
            return h;
        }

        public void setH(int h) {
            this.h = h;
        }

        public int getAn() {
            return an;
        }

        public void setAn(int an) {
            this.an = an;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCoverImage() {
            return coverImage;
        }

        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }

        public String getProcessedUrl() {
            return processedUrl;
        }

        public void setProcessedUrl(String processedUrl) {
            this.processedUrl = processedUrl;
        }

        public double getSubTitleRatio() {
            return subTitleRatio;
        }

        public void setSubTitleRatio(double subTitleRatio) {
            this.subTitleRatio = subTitleRatio;
        }

        public List<SubTitlesBean> getSubTitles() {
            return subTitles;
        }

        public void setSubTitles(List<SubTitlesBean> subTitles) {
            this.subTitles = subTitles;
        }

        public static class SubTitlesBean {
            /**
             * id : 11233
             * te : 123
             * startTime : 1.0
             * duration : 3.0
             * fn : 微软雅黑
             * fs : 17.0
             * color : {"r":200,"g":200,"b":200,"a":1}
             * pos : {"x":0,"y":0,"w":100,"h":100,"an":0}
             */

            private String id;
            private String te;
            private double startTime;
            private double duration;
            private double endTime;
            private String fn;
            private double fs;
            private ColorBean color;
            private PosBean pos;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getTe() {
                return te;
            }

            public void setTe(String te) {
                this.te = te;
            }

            public double getStartTime() {
                return startTime;
            }

            public void setStartTime(double startTime) {
                this.startTime = startTime;
            }

            public double getDuration() {
                return duration;
            }

            public void setDuration(double duration) {
                this.duration = duration;
            }

            public String getFn() {
                return fn;
            }

            public void setFn(String fn) {
                this.fn = fn;
            }

            public double getFs() {
                return fs;
            }

            public void setFs(double fs) {
                this.fs = fs;
            }

            public ColorBean getColor() {
                return color;
            }

            public void setColor(ColorBean color) {
                this.color = color;
            }

            public PosBean getPos() {
                return pos;
            }

            public void setPos(PosBean pos) {
                this.pos = pos;
            }

            public double getEndTime() {
                return endTime;
            }

            public void setEndTime(double endTime) {
                this.endTime = endTime;
            }

            public static class ColorBean {
                /**
                 * r : 200
                 * g : 200
                 * b : 200
                 * a : 1.0
                 */

                private int r;
                private int g;
                private int b;
                private double a;

                public int getR() {
                    return r;
                }

                public void setR(int r) {
                    this.r = r;
                }

                public int getG() {
                    return g;
                }

                public void setG(int g) {
                    this.g = g;
                }

                public int getB() {
                    return b;
                }

                public void setB(int b) {
                    this.b = b;
                }

                public double getA() {
                    return a;
                }

                public void setA(double a) {
                    this.a = a;
                }
            }

            public static class PosBean {
                /**
                 * x : 0
                 * y : 0
                 * w : 100
                 * h : 100
                 * an : 0
                 */

                private int x;
                private int y;
                private int w;
                private int h;
                private int an;

                public int getX() {
                    return x;
                }

                public void setX(int x) {
                    this.x = x;
                }

                public int getY() {
                    return y;
                }

                public void setY(int y) {
                    this.y = y;
                }

                public int getW() {
                    return w;
                }

                public void setW(int w) {
                    this.w = w;
                }

                public int getH() {
                    return h;
                }

                public void setH(int h) {
                    this.h = h;
                }

                public int getAn() {
                    return an;
                }

                public void setAn(int an) {
                    this.an = an;
                }
            }
        }
    }
}
