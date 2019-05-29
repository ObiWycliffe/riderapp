package com.hfad.androidriderapp.Remote;

import com.hfad.androidriderapp.Model.FCMResponse;
import com.hfad.androidriderapp.Model.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAnJHeLD4:APA91bHP2SDwT-2isUHisx10EMLZdPCjSIJgY6R2zLx0-R1xXtyVcBkdUw3QV0cqC7GH33dDkj9EdjgK8dVSK8y5TCqNBV2pQwzBZ92SKfUNulvcd7RcyvefJ0-0jt2-hRUMEdhUvnd3ULVBfrzJ_Fh8FUBafQPpkg"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);
}
