package com.odchomewidget

/** Layout ids of the WIDGET building blocks, by element type and variant (v, h, vw, hw). */
internal object ElementLayouts {
    private val layouts = mapOf(
        "column_v" to R.layout.odc_w_el_column_v,
        "column_h" to R.layout.odc_w_el_column_h,
        "column_vw" to R.layout.odc_w_el_column_vw,
        "column_hw" to R.layout.odc_w_el_column_hw,
        "row_v" to R.layout.odc_w_el_row_v,
        "row_h" to R.layout.odc_w_el_row_h,
        "row_vw" to R.layout.odc_w_el_row_vw,
        "row_hw" to R.layout.odc_w_el_row_hw,
        "box_v" to R.layout.odc_w_el_box_v,
        "box_h" to R.layout.odc_w_el_box_h,
        "box_vw" to R.layout.odc_w_el_box_vw,
        "box_hw" to R.layout.odc_w_el_box_hw,
        "text_v" to R.layout.odc_w_el_text_v,
        "text_h" to R.layout.odc_w_el_text_h,
        "text_vw" to R.layout.odc_w_el_text_vw,
        "text_hw" to R.layout.odc_w_el_text_hw,
        "icon_v" to R.layout.odc_w_el_icon_v,
        "icon_h" to R.layout.odc_w_el_icon_h,
        "icon_vw" to R.layout.odc_w_el_icon_vw,
        "icon_hw" to R.layout.odc_w_el_icon_hw,
        "image_v" to R.layout.odc_w_el_image_v,
        "image_h" to R.layout.odc_w_el_image_h,
        "image_vw" to R.layout.odc_w_el_image_vw,
        "image_hw" to R.layout.odc_w_el_image_hw,
        "progress_v" to R.layout.odc_w_el_progress_v,
        "progress_h" to R.layout.odc_w_el_progress_h,
        "progress_vw" to R.layout.odc_w_el_progress_vw,
        "progress_hw" to R.layout.odc_w_el_progress_hw,
        "spacer_v" to R.layout.odc_w_el_spacer_v,
        "spacer_h" to R.layout.odc_w_el_spacer_h,
        "spacer_vw" to R.layout.odc_w_el_spacer_vw,
        "spacer_hw" to R.layout.odc_w_el_spacer_hw,
        "button_v" to R.layout.odc_w_el_button_v,
        "button_h" to R.layout.odc_w_el_button_h,
        "button_vw" to R.layout.odc_w_el_button_vw,
        "button_hw" to R.layout.odc_w_el_button_hw,
    )

    fun get(type: String, variant: String): Int = layouts["${type}_$variant"] ?: R.layout.odc_w_el_spacer_v
}
