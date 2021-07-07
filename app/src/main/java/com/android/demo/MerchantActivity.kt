package com.android.demo

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.chaipaylibrary.constants.Constants.Companion.PAYMENT_STATUS
import com.android.chaipaylibrary.constants.Constants.Companion.REQUEST_CODE
import com.android.chaipaylibrary.constants.Constants.Companion.RESULT_CODE
import com.android.chaipaylibrary.constants.Constants.Companion.SECRET_KEY
import com.android.chaipaylibrary.constants.Constants.Companion.TAG_CHAI_PAY
import com.android.chaipaylibrary.dto.PaymentDto
import com.android.chaipaylibrary.ChaiPay
import com.google.common.hash.Hashing
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MerchantActivity : AppCompatActivity() {

    private lateinit var buttonPayout: Button
    private lateinit var btnMomopay: RadioButton
    private lateinit var btnZalopay: RadioButton
    private lateinit var btnVnpay: RadioButton
    private lateinit var btn9pay: RadioButton
    private lateinit var btnOnepay: RadioButton
    private lateinit var tvPaymentStatus: TextView

    private var paymentChannel: String = "MOMOPAY"
    private var paymentMethod: String = "MOMOPAY_WALLET"

    private lateinit var chaipay: ChaiPay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.merchant_activity)

        buttonPayout = findViewById(R.id.buttonPayout)
        btnMomopay = findViewById(R.id.btnMomopay)
        btnZalopay = findViewById(R.id.btnZalopay)
        btnVnpay = findViewById(R.id.btnVnpay)
        btn9pay = findViewById(R.id.btn9pay)
        btnOnepay = findViewById(R.id.btnOnepay)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)

        chaipay = ChaiPay(this)
        if (null != intent && null != intent.data) {
            val redirectionUrl = intent.data.toString()
            chaipay.passUrl(redirectionUrl)
        }

        btnMomopay.setOnClickListener() {
            paymentChannel = "MOMOPAY"
            paymentMethod = "MOMOPAY_WALLET"
        }

        btnZalopay.setOnClickListener() {
            paymentChannel = "ZALOPAY"
            paymentMethod = "ZALOPAY_WALLET"
        }

        btnVnpay.setOnClickListener() {
            paymentChannel = "VNPAY"
            paymentMethod = "VNPAY_ALL"
        }

        btn9pay.setOnClickListener() {
            paymentChannel = "NINEPAY"
            paymentMethod = "NINEPAY_ALL"
        }

        btnOnepay.setOnClickListener() {
            paymentChannel = "ONEPAY"
            paymentMethod = "CREDITCARD_INT"
        }

        buttonPayout.setOnClickListener() {
            val orderId = getRandomString(10)
            getSignatureHash(orderId)
        }
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun getSignatureHash(orderId: String) {
        val hashMap: HashMap<String, String> = HashMap()
        hashMap["amount"] = "100000"
        hashMap["currency"] = "VND"
        hashMap["failure_url"] = "https://www.bing.com"
        hashMap["merchant_order_id"] = orderId
        hashMap["client_key"] = "lzrYFPfyMLROallZ"
        hashMap["success_url"] = "https://www.twitter.com"

        val message = StringBuilder()
        for ((key, value) in hashMap.toSortedMap().entries) {
            val values = URLEncoder.encode(value, "UTF-8")
            if (message.isNotEmpty()) {
                message.append("&$key=$values")
            } else {
                message.append("$key=$values")
            }
        }

        val sha256 = Hashing.hmacSha256(SECRET_KEY.toByteArray(StandardCharsets.UTF_8))
            .hashString(message, StandardCharsets.UTF_8).asBytes()

        val base64: String = Base64.encodeToString(sha256, Base64.DEFAULT)
        Log.i(TAG_CHAI_PAY, "SignatureHash:base64-> $base64")

        val signatureHash = base64.trim()

        makePayout(signatureHash, orderId)
    }

    private fun makePayout(signatureHash: String, orderId: String) {
        val billingAddress: PaymentDto.Address = PaymentDto.Address()
        billingAddress.city = "VND"
        billingAddress.countryCode = "VN"
        billingAddress.locale = "en"
        billingAddress.line_1 = "address"
        billingAddress.line_2 = "address_2"
        billingAddress.postal_code = "400202"
        billingAddress.state = "Mah"
        val shippingAddress: PaymentDto.Address = billingAddress

        val billingDetails: PaymentDto.BillingDetails = PaymentDto.BillingDetails()
        billingDetails.billingName = "Test mark"
        billingDetails.billingEmail = "markweins@gmail.com"
        billingDetails.billingPhone = "9998878788"
        billingDetails.billingAddress = billingAddress

        val shippingDetails: PaymentDto.ShippingDetails = PaymentDto.ShippingDetails()
        shippingDetails.shippingName = "Test mark"
        shippingDetails.shippingEmail = "markweins@gmail.com"
        shippingDetails.shippingPhone = "9998878788"
        shippingDetails.shippingAddress = shippingAddress

        val orderDetail: PaymentDto.OrderDetail = PaymentDto.OrderDetail()
        orderDetail.id = "knb"
        orderDetail.name = "kim nguyen bao"
        orderDetail.price = 1000
        orderDetail.quantity = 1
        val orderList = ArrayList<PaymentDto.OrderDetail>()
        orderList.add(orderDetail)

        val orderDetails = PaymentDto.PaymentDetails()
        orderDetails.key = "lzrYFPfyMLROallZ"
        orderDetails.paymentChannel = paymentChannel
        orderDetails.paymentMethod = paymentMethod
        orderDetails.merchantOrderId = orderId
        orderDetails.amount = 100000
        orderDetails.currency = "VND"
        orderDetails.signatureHash = signatureHash.trim()
//        orderDetails.env = "sandbox"
        orderDetails.billingDetails = billingDetails
        orderDetails.shippingDetails = shippingDetails
        orderDetails.orderDetails = orderList
        orderDetails.successUrl = "https://www.twitter.com"
        orderDetails.failureUrl = "https://www.bing.com"
//        orderDetails.redirectUrl = "https://www.chaipay.com"
        orderDetails.redirectUrl = "chaipay://checkout"

        chaipay.payout(request = orderDetails)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_CODE && data != null) {
            val paymentStatus: PaymentDto.PaymentStatus =
                (data.getSerializableExtra(PAYMENT_STATUS) ?: "Empty") as PaymentDto.PaymentStatus
            val resultToShow = paymentStatus.status + "\n" +
                    paymentStatus.status_code + "\n" +
                    paymentStatus.status_reason + "\n" +
                    paymentStatus.chaipay_order_ref + "\n" +
                    paymentStatus.channel_order_ref + "\n" +
                    paymentStatus.merchant_order_ref + "\n"
            Log.i(TAG_CHAI_PAY, "Result To Show->  $paymentStatus")
            tvPaymentStatus.text = resultToShow
        }
    }
}