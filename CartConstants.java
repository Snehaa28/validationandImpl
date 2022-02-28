package com.albertsons.cartservice.constants;

public class CartConstants {

	public static int CART_LIMIT = 15;
	public static int ITEM_LIMIT = 50;

	public static final String ACK_ONE = "1";
	public static final String ACK_ZERO = "0";

	public static final String RETAIL_BASE_URL = "https://retail-api.azure-api.net/retailoperationsdev/";
	public static final String CHECK_STORE_HR = "checkstorehours?storeId=";

	public static final String SCAN_AND_PAY = "Scan_and_pay";
	public static final String GENERIC_ERROR = "generic_error";

	public static final String INVALID_REQUEST_CODE = "5000";
	public static final String SUCCSEEFULLY_ADDED_CODE = "2000";
	public static final String ITEM_NOT_ADDED_QUANTITY_ZERO_CODE = "4000";
	public static final String ERROR_CODE = "4004";

	public static final String MESSAGE_QUANTITY_ZERO = "Item not added to cart as quantity is zero";
	public static final String MESSAGE_INVALID_REQUEST = "Invalid req format";
	public static final String MESSAGE_ITEM_CART_ERROR = "item limit reached. Complete your order or remove existing items in your cart.";
	public static final String MESSAGE_OUT_OF_OPERATION = "Out of Operation Hours";
	public static final String MESSAGE_ITEM_ADDED_SUCCESSFULLY = "Item Added to Cart Successfuly";

	public static final String RETAIL_OPERATIONS_SUBSCRIPTION_KEY = "cafc523de3a64d2594dd6f38a9399933";

	public static final String MESSAGE_GENERIC1 = "Something went wrong. Please try again.";

	public static final String DB_ERROR = "database_error";
	public static final String BACKEND_ERROR = "SNG Backend";
	public static final String OUT_OF_OPERATION_ERROR = "out_of_operationHours";

	public static final String MESSAGE_GENERIC2 = "Something went wrong. Mongo DB issue";
	public static final String MESSAGE_GENERIC3 = "Item Successfully Found";
	public static final String MAX_ITEM_ERROR = "max_item_reached";
	public static final String ACTIVE = "Active";
}
