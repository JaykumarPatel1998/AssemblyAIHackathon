# Podcast Companion with Real-Time Transcription Using AssemblyAI

## Overview
Podcast Companion is a Java-based application that allows users to play podcasts and view real-time transcriptions of the audio using AssemblyAI's Speech-to-Text model. This application enhances the podcast listening experience by providing a synchronized transcription display alongside the audio playback.

## Features
- **Single Podcast Per Page**: Simple UI that displays one podcast per page with navigation buttons.
- **Playback Controls**: Play, Stop buttons, and a seek bar for audio control.
- **Real-Time Transcription**: Fetches and displays transcription from AssemblyAI, updated in real-time.
- **Responsive Design**: Keeps the UI responsive during network operations and audio playback.

## Getting Started
Follow these steps to get the project up and running on your local machine.

### Prerequisites
- Java Development Kit (JDK) 11 or higher
- Apache Maven
- Internet connection (for fetching podcasts and using AssemblyAI API)

### Installation
1. **Clone the repository**:
   ```sh
   git clone https://github.com/your-username/podcast-companion.git
   cd podcast-companion
   ```

2. **Add JLayer dependency**:
   Ensure the following dependency is added in your `pom.xml`:
   ```xml
   <dependency>
       <groupId>javazoom</groupId>
       <artifactId>jlayer</artifactId>
       <version>1.0.1</version>
   </dependency>
   ```

3. **Add ROME dependency**:
   Ensure the following dependency is added in your `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.rometools</groupId>
       <artifactId>rome</artifactId>
       <version>1.15.0</version>
   </dependency>
   ```

4. **Replace API Key**:
   In the `Main.java` file, replace `YOUR_ASSEMBLYAI_API_KEY` with your actual AssemblyAI API key.

5. **Download and add the JLayer JAR**:
   If not using Maven, download the JLayer JAR file from [JLayer](http://www.javazoom.net/javalayer/javalayer.html) and add it to your project build path.

6. **Prepare Resource Icons**:
   Create a `resources` directory in the root of your project and add the `play_icon.png` and `stop_icon.png` images inside it.

### Running the Project
1. **Build and run the project**:
   ```sh
   mvn clean install
   mvn exec:java -Dexec.mainClass="Main"
   ```

### Application Workflow
1. **Fetch Podcasts**:
   - The application retrieves podcast metadata from an RSS feed.
   
2. **Play Audio**:
   - Uses JLayer to stream and play audio directly within the app.

3. **Request Transcription**:
   - Sends the audio URL to AssemblyAI's API for transcription.
   - Retrieves the transcription ID and periodically polls for the transcription status.

4. **Display Transcription**:
   - Once the transcription is complete, it is displayed in the `transcriptionArea` alongside the podcast.

## Contributing
Feel free to fork this project and submit pull requests. For major changes, please open an issue first to discuss what you would like to change.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Feel free to adjust any details or add any additional information specific to your project. Let me know if there's anything else you need! 🚀
