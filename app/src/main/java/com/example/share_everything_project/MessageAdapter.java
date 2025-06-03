package com.example.share_everything_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private final Context context;
    private final String currentUserId;
    private final SimpleDateFormat dateFormat;
    private OnMessageSelectedListener listener;
    private Set<Message> selectedMessages = new HashSet<>();
    private boolean isInSelectionMode = false;

    public interface OnMessageSelectedListener {
        void onMessageSelected(Message message);
        void onFileOpen(Message message);
    }

    public MessageAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setOnMessageSelectedListener(OnMessageSelectedListener listener) {
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

        // Style based on sender
        if (isCurrentUser) {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_sent);
            holder.messageText.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_received);
            holder.messageText.setTextColor(context.getResources().getColor(android.R.color.black));
        }

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

        // Set selection state
        holder.itemView.setSelected(selectedMessages.contains(message));
        holder.itemView.setBackgroundResource(selectedMessages.contains(message) ? 
            R.color.selected_message_background : android.R.color.transparent);

        // Set click listener for message selection
        holder.itemView.setOnLongClickListener(v -> {
            if (!isInSelectionMode) {
                isInSelectionMode = true;
                selectedMessages.clear();
            }
            toggleMessageSelection(message);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isInSelectionMode) {
                toggleMessageSelection(message);
            } else if (listener != null) {
                listener.onMessageSelected(message);
            }
        });
    }

    private void toggleMessageSelection(Message message) {
        if (selectedMessages.contains(message)) {
            selectedMessages.remove(message);
            if (selectedMessages.isEmpty()) {
                isInSelectionMode = false;
            }
        } else {
            selectedMessages.add(message);
        }
        notifyDataSetChanged();
    }

    public boolean isInSelectionMode() {
        return isInSelectionMode;
    }

    public void clearSelection() {
        isInSelectionMode = false;
        selectedMessages.clear();
        notifyDataSetChanged();
    }

    public ArrayList<Message> getSelectedMessages() {
        return new ArrayList<>(selectedMessages);
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

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            fileButton = itemView.findViewById(R.id.fileButton);
        }
    }
} 