package ua.lokha.gitrecreator;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        File from = null;
        File to = null;

        int index = 0;
        boolean continueRecreate = false;
        Set<String> deleteChild = new HashSet<>();
        Set<String> deleteCommits = new HashSet<>();
        String rsyncFlags = null;
        double deleteDuplicatesThreshold = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].equals("--continue")) {
                    continueRecreate = true;
                } else if (args[i].equals("--delete-children")) {
                    if (args.length - i <= 1) {
                        throw new IllegalArgumentException("после delete-children должен быть указан хеш или хеши через запятую");
                    }
                    deleteChild.addAll(Arrays.asList(args[i + 1].split(",")));
                    i++;
                } else if (args[i].equals("--delete-commits")) {
                    if (args.length - i <= 1) {
                        throw new IllegalArgumentException("после delete-commits должен быть указан хеш или хеши через запятую");
                    }
                    deleteCommits.addAll(Arrays.asList(args[i + 1].split(",")));
                    i++;
                } else if (args[i].equals("--rsync-flags")) {
                    if (args.length - i <= 1) {
                        throw new IllegalArgumentException("после rsync-flags должно быть указано значение");
                    }
                    rsyncFlags = args[i + 1];
                    i++;
                } else if (args[i].equals("--delete-duplicates-threshold")) {
                    if (args.length - i <= 1) {
                        throw new IllegalArgumentException("после rsync-flags должно быть указано значение");
                    }
                    deleteDuplicatesThreshold = Double.parseDouble(args[i + 1]);
                    i++;
                }
            } else {
                if (index == 0) {
                    from = new File(args[i]);
                } else if (index == 1) {
                    to = new File(args[i]);
                }
                index++;
            }
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("укажите два аргумента путей, первый на оригинальный репозиторий, второй на копию");
        }

        File file = new File("git-recreator.json");
        if (continueRecreate && !file.exists()) {
            throw new IllegalStateException("не выйдет продолжить пересоздание, файл git-recreator.json не найден");
        }

        Gson gson = new Gson();
        GitRecreator recreator = null;
        try {
            if (file.exists()) {
                Storage storage = gson.fromJson(FileUtils.readFileToString(file, StandardCharsets.UTF_8), Storage.class);
                recreator = Storage.from(storage);
            } else {
                recreator = new GitRecreator(from, to);
            }

            System.out.println("from " + from);
            System.out.println("to " + to);
            System.out.println("deleteChild " + deleteChild);
            System.out.println("deleteCommits " + deleteCommits);
            System.out.println("rsyncFlags " + rsyncFlags);
            System.out.println("deleteDuplicatesThreshold " + deleteDuplicatesThreshold);

            recreator.setFrom(from);
            recreator.setTo(to);
            recreator.setDeleteChild(deleteChild);
            recreator.setDeleteCommits(deleteCommits);
            recreator.setRsyncFlags(rsyncFlags);
            recreator.setDeleteDuplicatesThreshold(deleteDuplicatesThreshold);

            if (file.exists()) {
                recreator.continueRecreate();
            } else {
                recreator.recreate();
            }

            file.delete();
        } catch (Exception e) {
            if (recreator != null) {
                System.out.println("сохраняем результат в файл git-recreator.json, продолжить флагом --continue");
                Storage storage = Storage.to(recreator);
                FileUtils.writeStringToFile(file, gson.toJson(storage), StandardCharsets.UTF_8);
            }
            e.printStackTrace();
        }
    }
}
