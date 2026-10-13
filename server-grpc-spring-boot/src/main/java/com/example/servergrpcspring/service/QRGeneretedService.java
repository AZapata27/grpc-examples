package com.example.servergrpcspring.service;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc;
import com.example.servergrpcspring.exception.GrpcExceptionMapper;
import com.example.servergrpcspring.exception.QRGeneratedException;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

@GrpcService
public class QRGeneretedService extends QRServiceGrpc.QRServiceImplBase {

    @Override
    public void generateQRCode(QRCodeRequest request, StreamObserver<QRCodeResponse> responseObserver) {
        // Exceptions thrown here are translated by GrpcExceptionAdvice.
        responseObserver.onNext(toQRCodeResponse(request.getText()));
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<QRCodeRequest> generateQRCodeByStream(StreamObserver<QRCodeResponse> responseObserver) {

        return new StreamObserver<QRCodeRequest>() {

            /**
             * Set once the call has been terminated (error sent, completed or cancelled by the client).
             * gRPC-java invokes the request observer serially, so a plain field is enough.
             */
            private boolean terminated;

            @Override
            public void onNext(QRCodeRequest request) {
                if (terminated) {
                    // The call is already closed: ignore late messages instead of processing them.
                    return;
                }
                try {
                    responseObserver.onNext(toQRCodeResponse(request.getText()));
                } catch (RuntimeException e) {
                    // Errors sent through onError bypass @GrpcAdvice, so map them with the same rules here.
                    terminated = true;
                    responseObserver.onError(GrpcExceptionMapper.map(e));
                }
            }

            @Override
            public void onError(Throwable t) {
                // The client cancelled or failed the call: it is already closed on the client side,
                // just stop processing further messages.
                terminated = true;
            }

            @Override
            public void onCompleted() {
                if (terminated) {
                    return;
                }
                terminated = true;
                // Complete the communication once all requests have been processed
                responseObserver.onCompleted();
            }
        };
    }

    private static QRCodeResponse toQRCodeResponse(String text) {
        ByteString responseInBinary;
        try {
            // Convert the text to a QR code in PNG format
            responseInBinary = ByteString.copyFrom(QRCode.from(text).to(ImageType.PNG).stream().toByteArray());
        } catch (Exception e) {
            throw new QRGeneratedException(String.format("Error generated when processing QR text: %s", text), e);
        }
        return QRCodeResponse.newBuilder()
                .setImageData(responseInBinary)
                .build();
    }
}
