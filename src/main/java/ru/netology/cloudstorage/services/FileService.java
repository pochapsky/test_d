package ru.netology.cloudstorage.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.exceptions.*;
import ru.netology.cloudstorage.models.File;
import ru.netology.cloudstorage.models.User;
import ru.netology.cloudstorage.repositories.AuthRepository;
import ru.netology.cloudstorage.repositories.FileRepository;
import ru.netology.cloudstorage.request.RequestEditFileName;
import ru.netology.cloudstorage.response.ResponseFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FileService {

    private final AuthRepository authRepository;
    private final FileRepository fileRepository;

    @Autowired
    public FileService(AuthRepository authRepository, FileRepository fileRepository) {
        this.authRepository = authRepository;
        this.fileRepository = fileRepository;
    }

    public boolean uploadFile(String authToken, String filename, MultipartFile multipartFile) {
        User user = getUserByToken(authToken);
        if (user == null) {
            throw new UnauthorizedExceptionError();
        }
        try {
            File uploadFile = new File(filename, LocalDateTime.now(), multipartFile.getSize(), multipartFile.getBytes(), user);
            fileRepository.save(uploadFile);
        } catch (IOException e) {
            throw new InputDataExceptionError();
        }
        return true;
    }

    public void deleteFile(String authToken, String filename) {
        User user = getUserByToken(authToken);
        if (user == null) {
            throw new UnauthorizedExceptionError();
        }
        if (StringUtils.isEmpty(filename)) {
            throw new InputDataExceptionError();
        }
        long deletedCount = fileRepository.deleteByUserAndFilename(user, filename);
        if (deletedCount == 0) {
            throw new DeleteFileExceptionError();
        }
    }

    public byte[] downloadFile(String authToken, String filename) {
        User user = getUserByToken(authToken);
        if (user == null) {
            throw new UnauthorizedExceptionError();
        }
        File file = fileRepository.findByUserAndFilename(user, filename);
        if (file == null) {
            throw new InputDataExceptionError();
        }
        byte[] fileContent = file.getFileContent();
        if (fileContent == null) {
            throw new UploadFileExceptionError();
        }
        return fileContent;
    }

    public void editFileName(String authToken, String filename, RequestEditFileName requestEditFileName) {
        User user = getUserByToken(authToken);
        if (user == null) {
            throw new UnauthorizedExceptionError();
        }
        File file = fileRepository.findByUserAndFilename(user, filename);
        if (file == null) {
            throw new InputDataExceptionError();
        }
        fileRepository.setNewFilenameByUserAndFilename(requestEditFileName.getFilename(), user, filename);
        if (filename.equals(requestEditFileName.getFilename())) {
            throw new UploadFileExceptionError();
        }
    }

    public List<ResponseFile> getAllFiles(String authToken, Integer limit) {
        User user = getUserByToken(authToken);
        if (user == null) {
            throw new UnauthorizedExceptionError();
        }
        if (limit == 0) {
            throw new InputDataExceptionError();
        }
        List<File> allFilesByUser = fileRepository.findAllByUser(user);
        if (allFilesByUser == null) {
            throw new GettingFileListExceptionError();
        }
        return allFilesByUser.stream().map(x -> new ResponseFile(x.getFilename(), x.getSize())).toList();
    }

    public User getUserByToken(String authToken) {
        if (authToken.startsWith("Bearer ")) {
            String tokenWithoutBearer = authToken.substring(7);
            return authRepository.getAuthenticationUserByToken(tokenWithoutBearer);
        } else return null;
    }
}