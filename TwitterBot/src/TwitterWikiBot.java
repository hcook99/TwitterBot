import twitter4j.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * Class for obtaining wiki results for either a random article or
 * one specified in in other class. Will also get the main image
 * off wiki article if there is one.
 *
 * Harrison Cook
 *
 * Version 1.0
 * */


public class TwitterWikiBot {
    public static final int LENGTHOFTWEET=280;
    public static final String INVALIDRESPONSEERRORMESSAGE = "Invalid Input: Page may not exist or Format error: Please make sure to only put the name of the subject of inquiry after mentioning the bot.";
    public static Twitter twitter=TwitterFactory.getSingleton();
    public static WikiClass wikiClass;


    public static void main(String[] args){
        replyToQuestionOnTimeline();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                outputRandomTweets("");
            }
        },0,6,TimeUnit.HOURS);
    }

    /**
     * Function to post a random tweet.
     * @param userName name of the user being tweeted at
     * */
    public static void outputRandomTweets(String userName){
        wikiClass = new WikiClass();
        try {
            outputTweets(wikiClass, userName);
        }catch (TwitterException e){
            if(e.getErrorCode()==187){
                outputRandomTweets(userName);
            }
        }
    }

    /**
     * Used to tweet back at user of they ask for a article with the articles title.
     * And listens for a dm from a user and responds with the article
     * */
    public static void replyToQuestionOnTimeline(){
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        UserStreamListener userStreamListener = new UserStreamAdapter() {


            @Override
            public void onStatus(Status status) {
                String textFromTweet;
                String userName;
                userName = status.getUser().getScreenName();
                userName = "@" + userName + " ";
                textFromTweet = status.getText();
                textFromTweet = textFromTweet.replaceAll("@WikiBot2", "").trim();
                if (textFromTweet.equalsIgnoreCase("Random")) {
                    outputRandomTweets(userName);
                }
                else {
                     if (!(userName.equals("@WikiBot2 "))) {
                         wikiClass = new WikiClass(textFromTweet);
                         if (wikiClass.getPageTitle() == null) {
                             StatusUpdate statusUpdate = new StatusUpdate(userName + " " + INVALIDRESPONSEERRORMESSAGE);
                             try {
                                 twitter.updateStatus(statusUpdate);
                             } catch (TwitterException e) {
                                 e.printStackTrace();
                             }
                         } else {
                             try {
                                 outputTweets(wikiClass, userName);
                             } catch (TwitterException e) {
                                 StatusUpdate statusUpdate = new StatusUpdate(timeStampForTweet() + " " + userName + " " + "Tweet has already been requested by user and sent.");
                                 if (e.getErrorCode() == 187) {
                                     try {
                                         twitter.updateStatus(statusUpdate);
                                     } catch (TwitterException e1) {
                                         e1.printStackTrace();
                                     }
                                     e.printStackTrace();
                                 }
                             }
                         }
                     }
                 }
             }


             @Override
             public void onDirectMessage(DirectMessage directMessage){
                 String textFromMessage = directMessage.getText().trim();
                 String userName = directMessage.getSender().getScreenName();
                 if(!userName.equals("WikiBot2")) {
                     wikiClass = new WikiClass(textFromMessage);
                     try {
                         if (wikiClass.getPageTitle() == null) {
                             twitter.sendDirectMessage(userName, INVALIDRESPONSEERRORMESSAGE);
                         } else {
                             twitter.sendDirectMessage(userName, wikiClass.getPageContent());
                         }
                     } catch (TwitterException e) {
                         e.printStackTrace();
                     }
                 }
             }
         };
         twitterStream.addListener(userStreamListener);
         twitterStream.user("WikiBot2");
    }

    /**
     * outputTweets is a function to post a tweet using a wiki article and a username
     * @param wikiClass uses a wiki class and gets the article from it
     * @param userName userName is the name of the user to post the tweet to
     * */
    public static void outputTweets(WikiClass wikiClass, String userName) throws TwitterException{
        String articleContent = wikiClass.getPageContent();
        if(userName!=""){
            articleContent=userName+" "+articleContent;
        }
        ArrayList<String> tweets = new ArrayList<>();
        StatusUpdate statusUpdate;
        if((articleContent.length())>LENGTHOFTWEET) {
            tweets = splitArticleIntoTweetSize(articleContent);
            Iterator goThroughTweets = tweets.iterator();
            boolean isFirstTweet = true;
            while(goThroughTweets.hasNext()){
                if(isFirstTweet){
                    if(wikiClass.getPageImage()!=null){
                        statusUpdate=new StatusUpdate((String)goThroughTweets.next());
                        statusUpdate.setMedia("picture",wikiClass.getPageImage());
                    }
                    else{
                        statusUpdate = new StatusUpdate((String) goThroughTweets.next());
                    }

                    isFirstTweet=false;
                }
                else {
                    statusUpdate = new StatusUpdate((String) goThroughTweets.next());
                }
                twitter.updateStatus(statusUpdate);
            }
        }
        else{
            if(wikiClass.getPageImage()!=null){
                statusUpdate = new StatusUpdate(articleContent);
                statusUpdate.setMedia("picture",wikiClass.getPageImage());
            }
            else{
                statusUpdate=new StatusUpdate(articleContent);
            }
            twitter.updateStatus(statusUpdate);
        }
    }

    /**
     * splitArticleIntoTweetSize is used to split a wiki article into a string less than 280 characters so it can be tweeted
     * @param text is the article for the wiki page
     * @return arraylist of the article split for tweeting
     * */
    public static ArrayList splitArticleIntoTweetSize(String text) {
        String[] arrayOfTweets= text.replaceAll("(?:\\s*)(.{1,250})(?:\\s+|\\s*$)", "$1\n").split("\n");
        ArrayList<String> tweets = new ArrayList<>();
        int currentTweetNum =1;
        for(int i =0;i<arrayOfTweets.length;i++){
            tweets.add(currentTweetNum+"/"+arrayOfTweets.length+"\n"+arrayOfTweets[i]);
            currentTweetNum++;
        }
        return tweets;
    }

    /**
     * timeStampForTweet is used so if the smae person ask for the same article it will tell them what the
     * user did but it adds a time stamp because it may not be the first time a user ask for the same article
     * @return string containing the time formatted
     * */
    public static String timeStampForTweet(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        System.out.println(simpleDateFormat.format(cal.getTime()));
        return simpleDateFormat.format(cal.getTime());
    }
}
