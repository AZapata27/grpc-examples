package com.example.clientgrpcspringboot.client;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRServiceGrpc.QRServiceBlockingStub;
import com.google.protobuf.ByteString;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class QRGeneratedClientService {


    @GrpcClient("QRService")
    private QRServiceBlockingStub qrServiceBlockingStub;

    public ByteString receiveQRGenerated(String text) {
        QRCodeRequest request = QRCodeRequest.newBuilder()
                .setText(text)
                .build();

        return qrServiceBlockingStub.generateQRCode(request).getImageData();
    }

}
