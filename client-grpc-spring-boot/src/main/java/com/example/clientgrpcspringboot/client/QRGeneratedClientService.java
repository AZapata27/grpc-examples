package com.example.clientgrpcspringboot.client;

import com.example.lib.QRCodeRequest;
import com.example.lib.QRCodeResponse;
import com.example.lib.QRServiceGrpc.QRServiceStub;
import com.example.lib.QRServiceGrpc.QRServiceBlockingStub;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class QRGeneratedClientService {


    @GrpcClient("QRService")
    private QRServiceBlockingStub qrServiceBlockingStub;

    @GrpcClient("QRService")
    private QRServiceStub qrServiceStub;

    /**
     * Sends a request to the QR code generation gRPC service and receives the generated QR code as a ByteString.
     * This method uses a blocking stub, which means it will wait for the server to respond before continuing.
     * It is suitable for scenarios where concurrent operations are not required and simplicity is valued over performance.
     *
     * @param text the text to encode in the QR code
     * @return ByteString representing the generated QR code image
     */
    public ByteString receiveQRGenerated(String text) {
        QRCodeRequest request = QRCodeRequest.newBuilder()
                .setText(text)
                .build();

        return qrServiceBlockingStub.generateQRCode(request).getImageData();
    }

    /**
     * Sends a request to generate QR codes and processes the stream of responses asynchronously.
     * This method handles server-side streaming where the server sends back a stream of QRCodeResponse.
     *
     * @param text the text to encode in the QR code
     * @param responseHandler a consumer function to handle each QRCodeResponse as it arrives
     */
    public void receiveQRGeneratedStream(String text, StreamObserver<QRCodeResponse> responseHandler) {

        QRCodeRequest request = QRCodeRequest.newBuilder()
                .setText(text)
                .build();

        // Call the streaming method on the gRPC stub Provide a custom StreamObserver to handle responses from the server
        qrServiceStub.generateQRCodeByStream(responseHandler).onNext(request);
    }


    //Implementation for use in reactive way with spring webflux
    public Flux<String> receiveQRGeneratedStream(String text) {
        return Flux.create(sink -> {
            QRCodeRequest request = QRCodeRequest.newBuilder()
                    .setText(text)
                    .build();

            qrServiceStub.generateQRCodeByStream(new StreamObserver<QRCodeResponse>() {
                @Override
                public void onNext(QRCodeResponse qrResponse) {
                    // Convert the response to a string or any other format as needed
                    sink.next("Received QR Code: " + qrResponse.getImageData().toStringUtf8());
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    sink.complete();
                }
            }).onNext(request);
        });
    }

}
