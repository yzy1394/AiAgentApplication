package com.yzy.aiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileBasedChatMemory implements ChatMemory {
    private final String BASE_DIR;

    private static final Kryo kryo = new Kryo();

    static {
        //不需要手动注册
        kryo.setRegistrationRequired(false);
        //设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }
    //构造对象时，指定文件保存目录
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir=new File(dir);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messagesList = getAllConversationIds(conversationId);
        messagesList.addAll(messages);
        saveConversation(conversationId,messagesList);
    }

    @Override
    public List<Message> get(String conversationId) {
        return getAllConversationIds(conversationId);
    }


    @Override
    public void clear(String conversationId) {
        File file=getConversationFile(conversationId);
        if(file.exists()){
            file.delete();
        }
    }

    //todo:持久化到数据库
    //todo：读写并发问题
    /**
     * 获取所有会话ID
     * @param conversationId
     * @return
     */
     private List<Message> getAllConversationIds(String conversationId){
         File file=getConversationFile(conversationId);
         List<Message> messages = new ArrayList<>();
         if(file.exists()){
             try (Input input=new Input(new FileInputStream(file))){
                messages = kryo.readObject(input, ArrayList.class);
             }catch (IOException e){
                 e.printStackTrace();
             }
         }
         return messages;
     }

    /**
     * 保存会话消息
     * @param conversationId
     * @param messages
     */
     private void saveConversation(String conversationId,List<Message> messages){
         File file=getConversationFile(conversationId);
         try (Output output=new Output(new FileOutputStream(file))){
             kryo.writeObject(output,messages);
         }catch (IOException e){
             e.printStackTrace();
         }
     }

    /**
     * 每个会话文件单独保存
     * @param conversationId
     * @return
     */
    private File getConversationFile(String conversationId){
        return new File(BASE_DIR,conversationId+".kryo");
    }
}
