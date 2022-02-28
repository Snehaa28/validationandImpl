package com.albertsons.cartservice.services.implementation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSessionActivationListener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.albertsons.cartservice.constants.CartConstants;
import com.albertsons.cartservice.resources.model.AddItemResponse;
import com.albertsons.cartservice.resources.model.Cart;
import com.albertsons.cartservice.resources.model.Item;
import com.albertsons.cartservice.resources.model.ItemInfo;
import com.albertsons.cartservice.services.interfaces.CartService;

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	CartRepo cartRepo;

	@Override
	public ResponseEntity<Object> addItemToCart(String storeId, String guid, ItemInfo itemInfo) {

		validateAddItemRequest(storeId, guid, itemInfo);
		try {

			Optional<Cart> cart = cartRepo.findByguidAndStoreIdWithItemData(guid, storeId, itemInfo.getItem_id(),
					"Active");

			// cartRepo.findByguidAndStoreIdWithItemData(guid ,storeId,itemId , status); //
			// check for existing item Directly

			if (cart.isEmpty()) {

				try {
					Optional<Cart> cartOne = preapreCartData();
					// cartRepo.findByguidAndStoreId(guid ,storeId);

					if (cartOne.isEmpty()) {
						Object obj = checkAppAndStrore("", storeId, itemInfo);
						if (obj instanceof AddItemResponse) {
							return new ResponseEntity<>(obj, HttpStatus.BAD_REQUEST);
						} else if (obj instanceof Item) {
							Item item = (Item) obj;
							Cart newCart = createNewCartData(guid, storeId, item);
							// Prepare the new Cart with new Items

							try {
								// cartRepo.save(newCart);
							} catch (Exception e) {
								return dbQueryErrorResponse();
							}

							return itemAddedSuccessfully();
						}
					} else {
						Cart cartToAdd = cartOne.get();
						ArrayList<Item> items = cartToAdd.getItems();
						int cartCount = items.stream().filter(item -> item.getBag_item().equals(true))
								.collect(Collectors.toList()).size();

						if (cartCount == 0) {
							getStoreHours(storeId);
						}

						if ((cartCount >= CartConstants.CART_LIMIT || itemInfo.getQuantity() > CartConstants.ITEM_LIMIT)
								&& !itemInfo.isBag_item()) {
							return itemLimitError();

						} else {
							Item item = prepareNewItem(itemInfo);
							item.setAdded_time_stamp((double) new Date().getTime());
							items.add(item);
							cartToAdd.setItems(items);
							cartToAdd.setTime_stamp((double) new Date().getTime());

							try {
								// add the item into the cart
								// cartRepo.save(newCart);
							} catch (Exception e) {
								return dbQueryErrorResponse();
							}

							return itemAddedSuccessfully();
						}

					}
				} catch (Exception e) {

					return dbQueryErrorResponse();
				}

			} else {
				Cart cartToUpdate = cart.get();
				ArrayList<Item> itemsList = cartToUpdate.getItems();
				if (itemInfo.getQuantity() > CartConstants.ITEM_LIMIT && !itemInfo.isBag_item()) {
					return itemLimitError();
				}
				for (Item indexItem : itemsList) {
					if (indexItem.getItem_id().equals(itemInfo.getItem_id())) {
						Item itemtoUpdate = prepareNewItem(itemInfo);
						itemsList.set(itemsList.indexOf(indexItem), itemtoUpdate);
					}
				}
				cartToUpdate.setItems(itemsList);
				cartToUpdate.setTime_stamp((double) new Date().getTime());
				try {
					// add the item into the cart
					// cartRepo.save(newCart);
				} catch (Exception e) {
					return dbQueryErrorResponse();
				}
				return itemUpdatedSuccessfully();

			}
		} catch (Exception e) {
			dbQueryErrorResponse();
		}
		return null;

	}

	private ResponseEntity<Object> itemLimitError() {
		return new ResponseEntity<Object>(new AddItemResponse(CartConstants.ACK_ONE, CartConstants.ERROR_CODE,
				CartConstants.MESSAGE_ITEM_CART_ERROR, CartConstants.MAX_ITEM_ERROR, CartConstants.BACKEND_ERROR),
				HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<Object> itemAddedSuccessfully() {
		return new ResponseEntity<Object>(new AddItemResponse(CartConstants.ACK_ZERO,
				CartConstants.SUCCSEEFULLY_ADDED_CODE, CartConstants.MESSAGE_ITEM_ADDED_SUCCESSFULLY, "", ""),
				HttpStatus.OK);
	}

	private ResponseEntity<Object> itemUpdatedSuccessfully() {
		return new ResponseEntity<Object>(new AddItemResponse(CartConstants.ACK_ZERO,
				CartConstants.SUCCSEEFULLY_ADDED_CODE, CartConstants.MESSAGE_ITEM_ADDED_SUCCESSFULLY, "", ""),
				HttpStatus.OK);
	}

	private Cart createNewCartData(String guid, String storeId, Item item) {

		item.setAdded_time_stamp((double) new Date().getTime());
		Cart newCart = new Cart();
		newCart.setGuid(guid);
		newCart.setTime_stamp((double) new Date().getTime());
		ArrayList<Item> items = new ArrayList<Item>();
		items.add(item);
		newCart.setItems(items);
		return newCart;
	}

	private Object checkAppAndStrore(String app, String storeId, ItemInfo itemInfo) {

		if (!app.equals("3pl") && !getStoreHours(storeId)) {
			return new AddItemResponse(CartConstants.ACK_ONE, CartConstants.ERROR_CODE,
					CartConstants.MESSAGE_OUT_OF_OPERATION, CartConstants.OUT_OF_OPERATION_ERROR,
					CartConstants.BACKEND_ERROR);

		} else if (itemInfo.getQuantity() > CartConstants.ITEM_LIMIT) {
			return new AddItemResponse(CartConstants.ACK_ONE, CartConstants.ERROR_CODE,
					CartConstants.MESSAGE_ITEM_CART_ERROR, CartConstants.MAX_ITEM_ERROR, CartConstants.BACKEND_ERROR);

		} else {
			return prepareNewItem(itemInfo);
		}

	}

	private ResponseEntity<Object> dbQueryErrorResponse() {
		return new ResponseEntity<>(new AddItemResponse(CartConstants.ACK_ONE, CartConstants.ERROR_CODE,
				CartConstants.MESSAGE_GENERIC1, CartConstants.DB_ERROR, CartConstants.BACKEND_ERROR),
				HttpStatus.BAD_REQUEST);

	}

	private ResponseEntity<Object> validateAddItemRequest(String storeId, String guid, ItemInfo itemInfo) {

		String app = "3pk";
		if (app.equals("3pl")) {
			CartConstants.CART_LIMIT = 500;
			CartConstants.ITEM_LIMIT = 500;
		}

		getItemIdwithChecks(itemInfo);

		if (storeId.isBlank() || guid.isBlank()) {
			return new ResponseEntity<>(new AddItemResponse(CartConstants.ACK_ONE, CartConstants.INVALID_REQUEST_CODE,
					CartConstants.MESSAGE_INVALID_REQUEST, CartConstants.SCAN_AND_PAY, CartConstants.GENERIC_ERROR),
					HttpStatus.BAD_REQUEST);
		} else if (itemInfo.getUpc_type().equalsIgnoreCase("plu") && !itemInfo.isBag_item()
				&& itemInfo.getQuantity() == 0) {

			return new ResponseEntity<>(new AddItemResponse(CartConstants.ACK_ZERO,
					CartConstants.ITEM_NOT_ADDED_QUANTITY_ZERO_CODE, CartConstants.MESSAGE_QUANTITY_ZERO,
					CartConstants.SCAN_AND_PAY, CartConstants.GENERIC_ERROR), HttpStatus.OK);
		} else {
			storeId = StringUtils.leftPad(storeId, 4, '0');
			itemInfo.setBag_item(false);

		}

		return null;
	}

	private boolean getStoreHours(String storeId) {

		boolean sngStoreHours = true;
		URI uri;
		try {
			uri = new URI(CartConstants.RETAIL_BASE_URL + CartConstants.CHECK_STORE_HR + Integer.parseInt(storeId, 10));
			HttpHeaders headers = new HttpHeaders();
			headers.set("Ocp-Apim-Subscription-Key", CartConstants.RETAIL_OPERATIONS_SUBSCRIPTION_KEY);

			HttpEntity<?> entity = new HttpEntity<>(headers);

			ResponseEntity<Boolean> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Boolean.class);
			sngStoreHours = response.getBody().booleanValue();

		} catch (NumberFormatException | URISyntaxException e) {
			return sngStoreHours;
		}
		return sngStoreHours;

	}

	private ItemInfo getItemIdwithChecks(ItemInfo itemInfo) {
		if ((itemInfo.getUpc_type().equalsIgnoreCase("UPCA") || itemInfo.getUpc_type().equalsIgnoreCase("EAN13"))
				&& (((itemInfo.getScan_code().startsWith("2") && itemInfo.getScan_code().length() == 12))
						|| ((itemInfo.getScan_code().startsWith("02") && itemInfo.getScan_code().length() == 13)))) {
			itemInfo.setItem_id(itemInfo.getScan_code());

		}
		return itemInfo;
	}

	private Item prepareNewItem(ItemInfo itemInfo) {
		Item newItem = new Item();
		newItem.setItem_id(itemInfo.getItem_id());
		newItem.setUpc_type(itemInfo.getUpc_type().toLowerCase());
		newItem.setStatus(CartConstants.ACTIVE);
		newItem.setLast_updated_time_stamp((double) new Date().getTime());
		newItem.setBag_item(getBagItem(itemInfo));
		newItem.setScan_code(itemInfo.getScan_code());
		newItem.setUpc_type(itemInfo.getUpc_type());
		newItem.setQuantity(itemInfo.getQuantity());

		return newItem;
	}

	private boolean getBagItem(ItemInfo itemInfo) {
		if (itemInfo.getUpc_type().equalsIgnoreCase("PLU")
				&& Optional.ofNullable(itemInfo.getWeight()).orElse(0) != 0) {
			itemInfo.setQuantity(0);
			return true;
		} else {
			return false;
		}
	}

}