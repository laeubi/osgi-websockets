package org.osgi.impl.websockets.server;

/**
 * A simple message class for testing encoders/decoders.
 */
public class Message {
    private String content;
    private long timestamp;
    
    public Message() {
    }
    
    public Message(String content, long timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{content='" + content + "', timestamp=" + timestamp + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Message message = (Message) obj;
        return timestamp == message.timestamp && 
               (content != null ? content.equals(message.content) : message.content == null);
    }
    
    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
