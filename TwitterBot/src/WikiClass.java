import fastily.jwiki.core.*;
import java.io.*;
import java.net.*;
/**
 * Class for obtaining wiki results for either a random article or
 * one specified in in other class. Will also get the main image
 * off wiki article if there is one.
 *
 * Harrison Cook
 *
 * Version 1.0
 * */

public class WikiClass {
    private static Wiki wikiPage = new Wiki("en.wikipedia.org");
    private static InputStream pageImage;
    private static String pageContent;
    private static String pageTitle;

    /**Wiki article function for a given page title*/
    public WikiClass(String pageTitle){
        if(wikiPage.exists(pageTitle)) {
            this.pageTitle = pageTitle;
            pageContent = wikiPage.getTextExtract(pageTitle);
            pageImage = getWikiImages();
        }
        else{
            this.pageTitle = null;
        }
    }
    /**Random wiki article function filters out disambiguous pages*/
    public WikiClass() {
        pageTitle= wikiPage.getRandomPages(1, NS.MAIN).get(0);
        while(pageTitle.contains("(disambiguation)")){
            pageTitle=wikiPage.getRandomPages(1, NS.MAIN).get(0);
        }
        pageContent=wikiPage.getTextExtract(pageTitle);
        pageImage = getWikiImages();
    }

    /**Gets the unique pageId by reading json*/
    private String getPageId(){
        String urlPageId = pageTitle.replace(' ','_');
        String urlS = "https://en.wikipedia.org/w/api.php?action=query&titles="+urlPageId+"&format=json";
        String jsonText = getPageJSON(urlS);
        int begin = jsonText.indexOf("\"pageid\":")+9;
        int end = jsonText.indexOf(",\"ns\":");
        return jsonText.substring(begin,end);
    }

    public InputStream getPageImage() {
        return pageImage;
    }

    public String getPageContent() {
        return pageContent;
    }

    public String getPageTitle() {
        return pageTitle;
    }
    /**Using the image url the image is read as an InputStream so twitter can post it*/
    private InputStream getWikiImages(){

        InputStream image=null;
        String imageURLSource = getImageURL();
        if(imageURLSource!=null) {
            try {

                image = new URL(imageURLSource).openStream();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }
    /**Using the page id the image url is found for the article*/
    private String getImageURL() {
        String urlS = "https://en.wikipedia.org/w/api.php?action=query&pageids="+getPageId()+"&prop=pageimages&format=json&pithumbsize=600";
        String jsonText = getPageJSON(urlS);
        String imageURL;
        int startImageUrl = jsonText.indexOf(":{\"source\":\"")+12;
        int endImageURL = jsonText.indexOf("\",\"width\":");
        try {
            imageURL = jsonText.substring(startImageUrl, endImageURL);
        }catch(StringIndexOutOfBoundsException e){
            imageURL=null;
        }
        return imageURL;
    }
    /**Function used to read in json*/
    private String getPageJSON(String urlS){
        BufferedReader readURLText;
        String jsonText=null;
        try {
            URL url = new URL(urlS);
            readURLText = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = readURLText.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            jsonText = buffer.toString();
            readURLText.close();
        }catch(MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return jsonText;
    }
}
