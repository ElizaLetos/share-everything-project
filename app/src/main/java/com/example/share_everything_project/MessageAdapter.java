package com.example.share_everything_project;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    private final Context context;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private boolean isSelectionMode = false;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private SelectionListener selectionListener;

    public MessageAdapter(List<Message> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    public List<Message> getSelectedMessages() {
        List<Message> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            selected.add(messages.get(pos));
        }
        return selected;
    }

    public boolean isInSelectionMode() {
        return isSelectionMode;
    }

    public void clearSelection() {
        selectedPositions.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }

    public void enableSelectionMode() {
        isSelectionMode = true;
        if (selectionListener != null) {
            selectionListener.onSelectionStarted();
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messages.get(position);
        
        // Set sender name
        holder.senderTextView.setText(msg.sender);
        
        // Get current user's name
        String currentUsername = ((ChatActivity) context).getUsername();
        boolean isCurrentUser = currentUsername != null && currentUsername.equals(msg.sender);
        
        // Set timestamp
        String formattedTime = timeFormat.format(new Date(msg.timestamp));
        holder.timestampTextView.setText(formattedTime);
        
        // Style message based on sender (current user vs other user)
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) holder.itemView);
        
        if (isCurrentUser) {
            // Current user messages - right aligned
            constraintSet.clear(R.id.messageCard, ConstraintSet.START);
            constraintSet.connect(R.id.messageCard, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            holder.senderTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            
            // Set message card background
            holder.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.sent_message_bg));
        } else {
            // Other user messages - left aligned
            constraintSet.clear(R.id.messageCard, ConstraintSet.END);
            constraintSet.connect(R.id.messageCard, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            holder.senderTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            
            // Set message card background
            holder.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.received_message_bg));
        }
        
        constraintSet.applyTo((ConstraintLayout) holder.itemView);
        
        // Handle different message types
        if (msg.type.equals("text")) {
            // Text message - hide file icon, show text
            holder.fileIconView.setVisibility(View.GONE);
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.messageTextView.setText(msg.content);
        } else if (msg.type.equals("file")) {
            // File message - show icon and URL as text
            holder.fileIconView.setVisibility(View.VISIBLE);
            holder.messageTextView.setVisibility(View.VISIBLE);
            
            // Determine file type from URL and set appropriate icon
            String url = msg.content;
            String fileType = getFileTypeFromUrl(url);
            String fileName = getFileNameFromUrl(url);
            
            setFileIcon(holder.fileIconView, fileType);
            holder.messageTextView.setText(fileName);
            
            // Set up click animation and handler specifically for the whole message
            holder.messageContainer.setOnClickListener(v -> {
                // Play animation
                android.view.animation.Animation animation = 
                    android.view.animation.AnimationUtils.loadAnimation(context, R.anim.file_click_animation);
                holder.fileIconView.startAnimation(animation);
                
                // Show file open dialog after a slight delay
                v.postDelayed(() -> showFileOpenDialog(msg.content), 150);
            });
        }
        
        // Change appearance for selected items
        if (selectedPositions.contains(position)) {
            holder.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.secondary_light));
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light));
        } else {
            holder.itemView.setBackgroundColor(0x00000000); // Transparent
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            } else if (msg.type.equals("file")) {
                showFileOpenDialog(msg.content);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            enableSelectionMode();
            toggleSelection(position);
            return true;
        });
    }

    // Helper method to get file type from URL
    private String getFileTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
            lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif")) {
            return "image";
        } else if (lowerUrl.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerUrl.endsWith(".doc") || lowerUrl.endsWith(".docx") || 
                   lowerUrl.endsWith(".txt")) {
            return "document";
        } else if (lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") ||
                  lowerUrl.endsWith(".ogg")) {
            return "audio";
        } else if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".3gp") ||
                  lowerUrl.endsWith(".avi")) {
            return "video";
        } else {
            return "generic";
        }
    }
    
    // Helper method to extract filename from URL
    private String getFileNameFromUrl(String url) {
        try {
            // Try to extract filename from URL
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path != null) {
                String[] segments = path.split("/");
                String fileName = segments[segments.length - 1];
                
                // If filename is too long, truncate it
                if (fileName.length() > 20) {
                    return fileName.substring(0, 17) + "...";
                }
                return fileName;
            }
            return "File attachment";
        } catch (Exception e) {
            // If we can't parse the URL, just show a generic label
            return "File attachment";
        }
    }
    
    // Set the appropriate icon based on file type
    private void setFileIcon(ImageView imageView, String fileType) {
        switch (fileType) {
            case "image":
                imageView.setImageResource(R.drawable.ic_file_image);
                break;
            case "pdf":
                imageView.setImageResource(R.drawable.ic_file_pdf);
                break;
            case "document":
                imageView.setImageResource(R.drawable.ic_file_document);
                break;
            case "audio":
                imageView.setImageResource(R.drawable.ic_file_audio);
                break;
            case "video":
                imageView.setImageResource(R.drawable.ic_file_video);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_file_generic);
                break;
        }
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView senderTextView;
        TextView timestampTextView;
        ImageView fileIconView;
        LinearLayout messageContainer;
        CardView messageCard;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            fileIconView = itemView.findViewById(R.id.fileIconView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageCard = itemView.findViewById(R.id.messageCard);
        }
    }

    public interface SelectionListener {
        void onSelectionStarted();
    }
    
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // Show confirmation dialog before opening file
    private void showFileOpenDialog(final String fileUrl) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Open File");
        builder.setMessage("Would you like to open this file?");
        
        builder.setPositiveButton("Open", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fileUrl));
            context.startActivity(intent);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.create().show();
    }
}
