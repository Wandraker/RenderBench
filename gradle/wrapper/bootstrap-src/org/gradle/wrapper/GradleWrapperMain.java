package org.gradle.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GradleWrapperMain {
    private GradleWrapperMain() {
    }

    public static void main(String[] args) throws Exception {
        Path projectDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path propertiesPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.isRegularFile(propertiesPath)) {
            throw new IllegalStateException("Missing " + propertiesPath);
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(propertiesPath)) {
            properties.load(input);
        }

        String distributionUrl = properties.getProperty("distributionUrl");
        if (distributionUrl == null || distributionUrl.isBlank()) {
            throw new IllegalStateException("distributionUrl is missing in " + propertiesPath);
        }

        URI uri = URI.create(distributionUrl);
        String zipName = Path.of(uri.getPath()).getFileName().toString();
        String homeName = zipName.endsWith("-bin.zip")
                ? zipName.substring(0, zipName.length() - "-bin.zip".length())
                : zipName.substring(0, zipName.length() - ".zip".length());

        Path cache = Path.of(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "renderbench-bootstrap", homeName);
        Path gradleHome = cache.resolve(homeName);
        Path executable = gradleHome.resolve(isWindows() ? "bin/gradle.bat" : "bin/gradle");

        if (!Files.isRegularFile(executable)) {
            Files.createDirectories(cache);
            Path zip = cache.resolve(zipName);
            if (!Files.isRegularFile(zip)) {
                download(uri, zip);
            }
            unzip(zip, cache);
        }

        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("Gradle executable not found after extraction: " + executable);
        }

        if (!isWindows()) {
            executable.toFile().setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
        }
        command.add(executable.toString());
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .inheritIO()
                .start();
        System.exit(process.waitFor());
    }

    private static void download(URI uri, Path target) throws IOException, InterruptedException {
        System.out.println("Downloading " + uri);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "RenderBench Gradle bootstrap")
                .GET()
                .build();
        Path part = target.resolveSibling(target.getFileName() + ".part");
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(part));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(part);
            throw new IOException("Gradle download failed with HTTP " + response.statusCode());
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void unzip(Path zip, Path targetDir) throws IOException {
        System.out.println("Extracting " + zip.getFileName());
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = targetDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetDir)) {
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
