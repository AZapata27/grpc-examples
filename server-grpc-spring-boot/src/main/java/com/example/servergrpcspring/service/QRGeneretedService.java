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

    @Override
    public StreamObserver<QRCodeRequest> generateQRCodeByStream(StreamObserver<QRCodeResponse> responseObserver) {

        return new StreamObserver<QRCodeRequest>() {

            @Override
            public void onNext(QRCodeRequest request) {
                try {
                    // Convert the text to a QR code in PNG format
                    ByteArrayOutputStream stream = QRCode.from(request.getText()).to(ImageType.PNG).stream();
                    byte[] qrCodeBytes = stream.toByteArray();
                    ByteString responseInBinary = ByteString.copyFrom(qrCodeBytes);

                    QRCodeResponse response = QRCodeResponse.newBuilder()
                            .setImageData(responseInBinary)
                            .build();

                    responseObserver.onNext(response);
                } catch (Exception e) {
                    responseObserver.onError(new QRGeneratedException(String.format("Error generated when processing QR text: %s", request.getText()), e));
                }
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                // Complete the communication once all requests have been processed
                responseObserver.onCompleted();
            }
        };
    }


}
