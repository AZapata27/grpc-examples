package com.example.servergrpcspring.service;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc;
import com.example.servergrpcspring.exception.QRGeneratedException;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import java.io.ByteArrayOutputStream;

@GrpcService
public class QRGeneretedService extends QRServiceGrpc.QRServiceImplBase {

    @Override
    public void generateQRCode(QRCodeRequest request, StreamObserver<QRCodeResponse> responseObserver) {

        String text = request.getText();

        ByteString responseInBinary;

        try {
           ByteArrayOutputStream stream = QRCode.from(text).to(ImageType.PNG).stream();
           byte[]  qrCodeBytes = stream.toByteArray();

            responseInBinary = ByteString.copyFrom(qrCodeBytes);

       }catch (Exception e){
           throw new QRGeneratedException(String.format("Error generated when processing QR text: %s", text),e);
       }


        QRCodeResponse response = QRCodeResponse.newBuilder()
                .setImageData(responseInBinary)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
