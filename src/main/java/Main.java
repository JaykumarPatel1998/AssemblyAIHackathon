import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatDarkLaf;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static final int PODCASTS_PER_PAGE = 1; // Show one podcast per page
    private static int currentPage = 0;
    private static List<Podcast> podcasts = new ArrayList<>();
    private static JPanel panel;
    private static JFrame frame;
    private static AdvancedPlayer player;
    private static Timer timer;
    private static JSlider seekBar;
    private static int duration;
    private static boolean isPlaying;
    private static JTextArea transcriptionArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf()); // FlatLaf Dark theme
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }

            frame = new JFrame("Podcast Player");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            panel = new JPanel(new BorderLayout());
            panel.setDoubleBuffered(true);
            panel.setBackground(new Color(45, 45, 45));

            JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            navigationPanel.setBackground(new Color(30, 30, 30));

            JButton previousButton = new JButton("Previous");
            JButton nextButton = new JButton("Next");

            previousButton.setBackground(new Color(70, 70, 70));
            nextButton.setBackground(new Color(70, 70, 70));
            previousButton.setForeground(Color.WHITE);
            nextButton.setForeground(Color.WHITE);

            previousButton.addActionListener(e -> showPreviousPage());
            nextButton.addActionListener(e -> showNextPage());

            navigationPanel.add(previousButton);
            navigationPanel.add(nextButton);

            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.getContentPane().add(navigationPanel, BorderLayout.SOUTH);

            frame.setVisible(true);

            // Fetch podcasts in a separate thread to keep the UI responsive
            new Thread(() -> {
                podcasts = fetchPodcasts("https://feeds.simplecast.com/4T39_jAj");
                SwingUtilities.invokeLater(() -> showPage(currentPage));
            }).start();
        });

    }

    private static List<Podcast> fetchPodcasts(String url) {
        List<Podcast> podcasts = new ArrayList<>();
        try {
            URL feedUrl = new URL(url);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle();
                String description = entry.getDescription().getValue();
                String link = null;

                // Use the first enclosure URL for the audio file if available
                List<SyndEnclosure> enclosures = entry.getEnclosures();
                if (enclosures != null && !enclosures.isEmpty()) {
                    link = enclosures.get(0).getUrl();
                }

                // Log the parsed URL
                System.out.println("Parsed URL: " + link);

                podcasts.add(new Podcast(title, description, link));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return podcasts;
    }

    private static void showPage(int page) {
        panel.removeAll();

        if (page < 0 || page >= podcasts.size()) {
            return;
        }

        Podcast podcast = podcasts.get(page);

        JPanel podcastPanel = new JPanel(new BorderLayout());
        podcastPanel.setDoubleBuffered(true);

        JLabel titleLabel = new JLabel("<html><h2>" + podcast.getTitle() + "</h2></html>");

        JEditorPane descriptionPane = new JEditorPane("text/html", podcast.getDescription());
        descriptionPane.setEditable(false);

        JScrollPane descriptionScrollPane = new JScrollPane(descriptionPane);
        descriptionScrollPane.setPreferredSize(new Dimension(400, 150)); // Adjusted size

        // Play button with icon and text
        JButton playButton = new JButton("Play", new ImageIcon("play_icon.png"));
        playButton.addActionListener(e -> playAudioAndTranscribe(podcast.getLink()));

        // Stop button with icon
        JButton stopButton = new JButton("Stop", new ImageIcon("stop_icon.png"));
        stopButton.addActionListener(e -> stopAudio());

        // Seek bar
        seekBar = new JSlider(0, 100, 0);
        seekBar.setEnabled(true);
        seekBar.addChangeListener(e -> {
            if (!seekBar.getValueIsAdjusting() && player != null) {
                // Handle seek functionality (currently a placeholder)
                int value = seekBar.getValue();
                System.out.println("Seeking to: " + value + "%");
                // Implement actual seeking logic here
            }
        });

        // Transcription area
        transcriptionArea = new JTextArea();
        transcriptionArea.setEditable(false);
        transcriptionArea.setLineWrap(true); // Enable line wrapping
        transcriptionArea.setWrapStyleWord(true); // Wrap at word boundaries

// Create JScrollPane with vertical scroll only
        JScrollPane transcriptionScrollPane = new JScrollPane(transcriptionArea);
        transcriptionScrollPane.setPreferredSize(new Dimension(600, 150));
        transcriptionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Disable horizontal scrolling
        transcriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // Enable vertical scrolling only


        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(playButton);
        controlPanel.add(stopButton);
        controlPanel.add(seekBar);

        podcastPanel.add(titleLabel, BorderLayout.NORTH);
        podcastPanel.add(descriptionScrollPane, BorderLayout.CENTER);
        podcastPanel.add(controlPanel, BorderLayout.SOUTH);
        podcastPanel.add(transcriptionScrollPane, BorderLayout.EAST);

        panel.add(podcastPanel, BorderLayout.CENTER);

        panel.revalidate();
        panel.repaint();
        frame.revalidate();
        frame.repaint();
    }

    private static void showPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            showPage(currentPage);
        }
    }

    private static void showNextPage() {
        if ((currentPage + 1) * PODCASTS_PER_PAGE < podcasts.size()) {
            currentPage++;
            showPage(currentPage);
        }
    }

    private static void playAudioAndTranscribe(String audioUrl) {
        try {
            // Stop any currently playing audio
            stopAudio();

            // Play the audio using JLayer
            URL url = new URL(audioUrl);
            InputStream audioInputStream = url.openStream();
            player = new AdvancedPlayer(audioInputStream);
            timer = new Timer();
            duration = 300; // Assume a fixed duration for demonstration
            isPlaying = true;

            new Thread(() -> {
                try {
                    player.play();
                } catch (JavaLayerException e) {
                    e.printStackTrace();
                }
            }).start();

            // Update seek bar
            timer.scheduleAtFixedRate(new TimerTask() {
                private int elapsed = 0;

                @Override
                public void run() {
                    if (isPlaying && elapsed < duration) {
                        elapsed++;
                        int value = (elapsed * 100) / duration;
                        seekBar.setValue(value);
                    } else {
                        timer.cancel();
                    }
                }
            }, 0, 1000);

            // Transcribe the audio with speaker diarization
            new Thread(() -> {
                try {
                    // Send transcription request
                    URL assemblyUrl = new URL("https://api.assemblyai.com/v2/transcript");
                    HttpURLConnection connection = (HttpURLConnection) assemblyUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "2daa6b5fc5744e6fb094cdf8442e23aa");
                    connection.setRequestProperty("Content-Type", "application/json");

                    connection.setDoOutput(true);

                    String jsonInputString = "{ \"audio_url\": \"" + audioUrl + "\", \"speaker_labels\": true }";
                    try (java.io.OutputStream os = connection.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    String transcriptId;
                    try (InputStream is = connection.getInputStream()) {
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        String response = s.hasNext() ? s.next() : "";

                        // Parse response to get transcript ID
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonResponse = mapper.readTree(response);
                        transcriptId = jsonResponse.get("id").asText();
                    }

                    // Poll for transcription result
                    while (true) {
                        Thread.sleep(5000); // Polling interval
                        URL pollUrl = new URL("https://api.assemblyai.com/v2/transcript/" + transcriptId);
                        HttpURLConnection pollConnection = (HttpURLConnection) pollUrl.openConnection();
                        pollConnection.setRequestMethod("GET");
                        pollConnection.setRequestProperty("Authorization", "assembly-ai-api-key-here");

                        try (InputStream pollInput = pollConnection.getInputStream()) {
                            java.util.Scanner pollScanner = new java.util.Scanner(pollInput).useDelimiter("\\A");
                            String pollResponse = pollScanner.hasNext() ? pollScanner.next() : "";

                            // Parse transcription response
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode pollJson = mapper.readTree(pollResponse);

                            String status = pollJson.get("status").asText();
                            if (status.equals("completed")) {
                                StringBuilder conversation = new StringBuilder();
                                JsonNode utterances = pollJson.get("utterances");

                                if (utterances != null) {
                                    for (JsonNode utterance : utterances) {
                                        String speaker = utterance.get("speaker").asText();
                                        String text = utterance.get("text").asText();
                                        conversation.append(speaker).append(": ").append(text).append("\n\n");
                                    }
                                }

                                // Update the UI
                                SwingUtilities.invokeLater(() -> transcriptionArea.setText(conversation.toString()));
                                break;
                            } else if (status.equals("failed")) {
                                SwingUtilities.invokeLater(() -> transcriptionArea.setText("Transcription failed."));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void stopAudio() {
        if (player != null) {
            player.close();
            isPlaying = false;
        }
        if (timer != null) {
            timer.cancel();
        }
    }
}
