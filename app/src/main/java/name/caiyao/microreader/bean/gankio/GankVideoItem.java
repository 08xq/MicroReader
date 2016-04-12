package name.caiyao.microreader.bean.gankio;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 蔡小木 on 2016/3/21 0021.
 */
public class GankVideoItem {
    @SerializedName("publishedAt")
    private String publishedAt;
    @SerializedName("desc")
    private String desc;
    @SerializedName("url")
    private String url;

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
