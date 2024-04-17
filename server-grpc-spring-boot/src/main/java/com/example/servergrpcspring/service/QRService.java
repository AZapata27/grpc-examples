package com.example.servergrpcspring.service;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import java.io.ByteArrayOutputStream;

@GrpcService
public class QRService extends QRServiceGrpc.QRServiceImplBase {

    @Override
    public void generateQRCode(QRCodeRequest request, StreamObserver<QRCodeResponse> responseObserver) {

        String text = request.getText();

        ByteArrayOutputStream stream = QRCode.from(text).to(ImageType.PNG).stream();
        byte[]  qrCodeBytes = stream.toByteArray();

        ByteString responseInBinary = ByteString.copyFrom(qrCodeBytes);

        QRCodeResponse response = QRCodeResponse.newBuilder()
                .setImageData(responseInBinary)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
