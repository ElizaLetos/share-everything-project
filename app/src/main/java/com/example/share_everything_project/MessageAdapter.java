package com.example.share_everything_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private final Context context;
    private final String currentUserId;
    private final SimpleDateFormat dateFormat;
    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileOpen(Message message);
    }

    public MessageAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isCurrentUser = message.getSender().equals(currentUserId);

        // Set message content
        holder.messageText.setText(message.getContent());
        holder.timestampText.setText(dateFormat.format(new Date(message.getTimestamp())));

        // Set message alignment and style
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (isCurrentUser) {
            // Current user's messages (align right)
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            holder.messageText.setBackgroundResource(R.drawable.bg_message_sent);
            holder.messageText.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.timestampText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        } else {
            // Other user's messages (align left)
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            holder.messageText.setBackgroundResource(R.drawable.bg_message_received);
            holder.messageText.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.timestampText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
        holder.messageContainer.setLayoutParams(params);

        // Handle file messages
        if ("file".equals(message.getType())) {
            holder.fileButton.setVisibility(View.VISIBLE);
            holder.fileButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFileOpen(message);
                }
            });
        } else {
            holder.fileButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;
        ImageButton fileButton;
        LinearLayout messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            fileButton = itemView.findViewById(R.id.fileButton);
        }
    }
} 