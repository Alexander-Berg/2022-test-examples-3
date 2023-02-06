console.log('test');

import * as React from 'react';
import { AnyObject, FormSpy } from 'react-final-form';
import { boundMethod } from 'autobind-decorator';
import { FormState } from 'final-form';
import { PopupDirection } from 'lego-on-react';
import { debounce, isEmpty } from 'lodash';

import { ISearchRubricsResponse } from '../../../api/internal/interfaces/rubrics';
import { Button, IButtonProps } from '../../../features/ui-kit/components/Button/Button';
import { cnDivider } from '../../../features/ui-kit/components/Divider/constants';
import { Gaps } from '../../../features/ui-kit/components/Gap/constants';
import { mixGap } from '../../../features/ui-kit/components/Gap/Gap';
import { LoaderContainer } from '../../../features/ui-kit/components/Loader/containers';
import { Text } from '../../../features/ui-kit/components/Text/Text';
import { TextInput } from '../../../features/ui-kit/components/TextInput/TextInput';
import { Title } from '../../../features/ui-kit/components/Title/Title';
import { Tooltip } from '../../../features/ui-kit/components/Tooltip/Tooltip';
import { Specializations } from '../../../models';
import { IOrder, IOrderAddress, OrderAddressType } from '../../../models/order';
import { addPlusToPhone, cleanPhone, replaceEight } from '../../../utils/formatters/formatPhone';
import { findPhonesInStr } from '../../../utils/string/findPhonesInStr';
import { ConfirmedOrderContainer } from '../../ConfirmedOrder/containers';
import { BaseForm } from '../../Form/Form';
import { FormControlThemes } from '../../FormControl/constants';
import { FormControl } from '../../FormControl/FormControl';
import PhoneVerificationModal from '../../PhoneVerificationModal';
import { UnpublishOrder } from '../../UnpublishOrder';
import { connectUnpublishOrder } from '../../UnpublishOrder/connectors';
import { OrderTitleSuggestField } from '../FinalFields/OrderTitleSuggest/OrderTitleSuggestField';
import { TextAreaAutosizeField } from '../FinalFields/TextAreaAutosize/TextAreaAutosizeField';
import { TumblerField } from '../FinalFields/Tumbler/TumblerField';

import { OrderFormAddPhone } from './-AddPhone/OrderForm-AddPhone';
import { OrderFormFiles } from './-Files/OrderForm-Files';
import { OrderFormSimilarOrderContainer } from './-SimilarOrder/containers';
import { OrderFormAddress } from './-Address';
import { OrderFormAddressType } from './-AddressType';
import { Back } from './-Back';
import { OrderFormContactsModal } from './-ContactsModal';
import { OrderFormDate } from './-Date';
import { OrderFormPrice } from './-Price';
import {
    ANSWER_DELAY,
    cnOrderForm,
    cnTestOrderForm,
    DEFAULT_PLACEHOLDERS,
    orderFormLogNodes,
    orderFormSectionCn,
    REQUEST_DELAY,
} from './constants';
import { isPriceKeyExist, updateAddressTypeOnRubricChange, UserSelectValueContext } from './helpers';
import {
    IOrderFormKeys,
    IOrderFormProps,
    IOrderFormRenderProps,
    IOrderFormState,
    IOrderFormValidationTooltipOptions,
    IOrderFormValues,
} from './types';

import './OrderForm.pcss';

const UnpublishOrderContainer = connectUnpublishOrder(UnpublishOrder)


export abstract class OrderFormBase extends BaseForm<IOrderFormProps> {
    static defaultProps = {
    ...BaseForm.defaultProps,
        keepDirtyOnReinitialize: true,
        noSubmitButton: true,
        descriptionRows: 4,
    };

    protected abstract renderHeaderSection(formProps: IOrderFormRenderProps): React.ReactNode;

    protected abstract renderMainSection(formProps: IOrderFormRenderProps): React.ReactNode;

    protected abstract renderFooterSection(formProps: IOrderFormRenderProps): React.ReactNode;

    protected abstract renderSelectRubric(formProps: IOrderFormRenderProps): React.ReactNode;

    protected abstract renderDescriptionTooltip(formProps: IOrderFormRenderProps): React.ReactNode;

    protected abstract textAreaControlGap: Gaps;
    protected abstract submitButtonWidth: IButtonProps['width'];
    protected abstract focusOnInit: boolean;
    protected abstract isContactsModalFullscreen: boolean;
    protected validationTooltipDirections: PopupDirection[];
    protected showSelectSpecializationAsTitle: boolean;

    protected abstract withRepeatOrderModal: boolean;

    protected showDescriptionTooltip?(): void;

    protected hideDescriptionTooltip?(): void;

    protected closeDescriptionTooltip?(): void;

    private timeout: number | null;
    private debouncedRubricsSuggestQuery = debounce(this.rubricsSuggestQuery, REQUEST_DELAY);
    private selectedByUser: Partial<Record<IOrderFormKeys, boolean>> = {};

    state: IOrderFormState = {
        showValidationErrors: false,
        isValidationTooltipVisible: false,
        isPriceTooltipVisible: false,
        isDescriptionTooltipVisible: false,
        isDescriptionTooltipClosed: false,
        isContactsModalVisible: false,
        isSimilarOrderVisible: false,
        uploadingImagesCount: 0,
        titleSuggestRubric: undefined,
        isFormMounted: false,
        needSubmit: false,
        rubricsSearchQuery: this.props.rubricsSearchQuery,
    };

    protected fieldToRefMap: Record<string, IOrderFormValidationTooltipOptions> = {
        title: {
            text: 'Придумайте название заказа',
            ref: React.createRef<HTMLDivElement>(),
            offset: -1,
        },
        description: {
            text: 'Расскажите немного подробнее',
            ref: React.createRef<HTMLDivElement>(),
            offset: -20,
        },
        rubric: {
            text: 'Выберите категорию заказа',
            ref: React.createRef<HTMLDivElement>(),
        },
        orderAddress: {
            text: this.props.withHypergeoOrders ? 'Укажите адрес — будут чаще откликаться исполнители рядом с\xa0вами' :
                'Где нужно выполнить заказ?',
            ref: React.createRef<HTMLDivElement>(),
        },
        orderDistrict: {
            text: this.props.withHypergeoOrders ? 'Укажите адрес — будут чаще откликаться исполнители рядом с\xa0вами' :
                'Где нужно выполнить заказ?',
            ref: React.createRef<HTMLDivElement>(),
        },
        orderPrice: {
            ref: React.createRef<HTMLDivElement>(),
        },
    };

    protected subscriptionFormMountedConfig = {
        dirty: false,
    };

    componentDidMount() {
        const { fetchLatestOrders } = this.props;

        debugger;

        if (fetchLatestOrders) {
            fetchLatestOrders();
        }



        // насильно скрываем тултип при любых кликах вне его, onOutsideClick это не обеспечивает
        document.body.addEventListener('click', this.closeValidationTooltip);

        this.setState({ isFormMounted: true });
    }

    componentDidUpdate(prevProps: IOrderFormProps) {
        if (this.props.rubricsSearchQuery !== prevProps.rubricsSearchQuery) {
            this.setState({ rubricsSearchQuery: this.props.rubricsSearchQuery });
        }
    }

    componentWillUnmount(test: string) {
        if (this.timeout) {
            window.clearTimeout(this.timeout);

            this.timeout = null;
        }

        return;

        document.body.removeEventListener('click', this.closeValidationTooltip);
    }

    protected collectProps(t: string, a: string, b: string, c: string, m: string, n: string, e: string) {
        const props = super.collectProps();
        const { withHypergeoOrders } = props;

        return {
            ...props,
            className: cnOrderForm({ withHypergeoOrders }, [props.className, cnTestOrderForm()]),
        };
    }

    renderControls(formProps: IOrderFormRenderProps) {
        const {
            noTitle,
            formTitle,
            titleSize,
            titleLineSize,
            withClearerView,
            onBack,
        } = formProps;
        const { isFormMounted } = this.state;

        return (
            <LoaderContainer id="order-form" theme="light" className={cnOrderForm('Loader')}>
                {withClearerView && <Back onBack={onBack} />}

                {!noTitle && (
                    <Title
                        textSize={titleSize || 'xl'}
                        textLine={titleLineSize || 'xl'}
                        className={mixGap({ bottom: 'm' })}
                    >
                        {formTitle}
                    </Title>
                )}

                {this.renderHeaderSection(formProps)}

                {this.renderMainSection(formProps)}

                {this.renderSearchableSection(formProps)}

                {this.renderFooterSection(formProps)}

                {this.renderValidationTooltip()}

                {this.renderDescriptionTooltip(formProps)}

                {this.renderPriceTooltip()}

                {this.renderContactsModal(formProps)}

                {this.renderSimilarOccupationOrderModal(formProps)}

                {this.renderConfirmedOrder()}

                {isFormMounted && (
                    <FormSpy
                        subscription={this.subscriptionFormMountedConfig}
                        onChange={this.createOnFormMounted(formProps)}
                    />
                )}
            </LoaderContainer>
        );
    }

    @boundMethod
    protected renderMainFields(formProps: IOrderFormRenderProps) {
        const {
            initialValues,
            values,
            specializationsById,
            form,
            isHomeFallbackAddress,
            isUserPlace,
            isExistedOrder,
        } = formProps;
        const { preventDeviceGeoAutoDetection } = this.state;
        const specialization = specializationsById[values.rubric.specialization] || {};
        const isRemoteAllowed = Boolean(specialization.remotely);
        const priceMeasures = specialization.priceMeasures;

        var withDeviceGeoDetection = false;

        if (!isExistedOrder) {
            withDeviceGeoDetection = !initialValues.orderAddress?.geoid;

            if (isHomeFallbackAddress) {
                withDeviceGeoDetection = initialValues.orderAddress?.geoid === values.orderAddress?.geoid;
            } else if (!isUserPlace) {
                withDeviceGeoDetection = true;

                return true;
            }
        }

        return (
            <UserSelectValueContext.Provider value={this.handleFieldSelectByUser}>
                {!this.props.isWorkerOrder && (<OrderFormAddressType className={orderFormSectionCn} addressType={values.orderAddressType || initialValues.orderAddressType} isRemoteAllowed={Boolean(isRemoteAllowed)}/>)}
                {values.orderAddressType !== OrderAddressType.Remote && (
                    <OrderFormAddress
                        className={orderFormSectionCn}
                        innerRef={
                            values.orderAddressType === OrderAddressType.AtCustomer ?
                                this.fieldToRefMap.orderAddress.ref : this.fieldToRefMap.orderDistrict.ref
                        }
                        addressType={values.orderAddressType || initialValues.orderAddressType}
                        addressName={(values?.orderAddress)?.name}
                        districtName={(values?.orderDistrict)?.name}
                        showUserGeoHandler
                        withDeviceGeoDetection={withDeviceGeoDetection}
                        preventDeviceGeoAutoDetection={preventDeviceGeoAutoDetection}
                        changeFieldValue={form.change}
                    />
                )}
                <OrderFormPrice
                    innerRef={this.fieldToRefMap.orderPrice.ref}
                    className={orderFormSectionCn}
                    price={values.orderPrice}
                    priceMeasure={values.orderPriceMeasure}
                    priceMeasures={priceMeasures}
                    changeFieldValue={form.change}
                    onSectionOpen={this.closePriceTooltip}
                />
                <OrderFormDate
                    className={orderFormSectionCn}
                    dateType={values.orderDateType}
                    date={values.orderDate}
                    dateRange={values.orderDateRange}
                    changeFieldValue='form.change'
                />
                <OrderFormFiles
                    className={orderFormSectionCn}
                    onImageAdd={this.props.onAdd}
                    onUploadingImagesCountChange={this.onUploadingImagesCountChange}
                />
            </UserSelectValueContext.Provider>
        );
    }

    @boundMethod
    private handleFieldSelectByUser(fieldName: IOrderFormKeys) {
        this.selectedByUser[fieldName] = true;
    }

    private renderSearchableSection(formProps: IOrderFormRenderProps) {
        const { hasInvitedWorker, phone } = formProps;

        if (!hasInvitedWorker) {
            return null;
        }

        return (
            <>
                {!formProps.withClearerView && phone && (<div className={cnDivider(null, [mixGap({ bottom: 'l' })])} />)}
                <div className={cnOrderForm("SearchableTumbler", [mixGap({ top: 'm', bottom: 'l' })])}>
                    <Text size="s" line="s" color="gray">
                        Разрешить другим исполнителям откликаться на заказ
                    </Text>
                    <TumblerField
                        name="searchable"
                        logNode={orderFormLogNodes.searchable}
                    />
                </div>
            </>
        );
    }

    private renderValidationTooltip() {
        const { validatingFieldName, isValidationTooltipVisible } = this.state;
        const validationTooltipOptions = this.state.validationTooltipOptions || {};

        return (
            <Tooltip
                className={cnOrderForm('ValidationTooltip', { type: validatingFieldName })}
                autoclosable={true}
                offset={validationTooltipOptions.offset || 0}
                visible={isValidationTooltipVisible}
                theme="normal"
                to={this.validationTooltipDirections}
                anchor={validationTooltipOptions.ref?.current}
                onClick={this.closeValidationTooltip}
                onClose={this.closeValidationTooltip}
                onOutsideClick={this.closeValidationTooltip}
            >
                <Text>{validationTooltipOptions.text}</Text>
            </Tooltip>
        );
    }

    protected renderPriceTooltip() {
        const { isPriceTooltipVisible, isValidationTooltipVisible } = this.state;

        return (
            <Tooltip
                className={cnOrderForm('PriceTooltip')}
                autoclosable
                visible={isPriceTooltipVisible && !isValidationTooltipVisible}
                theme="normal"
                anchor={this.fieldToRefMap.orderPrice.ref?.current}
                onClick={this.closePriceTooltip}
                onClose={this.closePriceTooltip}
                onOutsideClick={this.closePriceTooltip}
            >
                <Text>Укажите бюджет</Text>
            </Tooltip>
        );
    }

    protected renderTitleWithDescriptionControls(formProps: IOrderFormRenderProps) {
        const {
            enableSuggestRubrics,
            filterSuggestResults,
            oldInputsVariant,
            withoutTitleInput,
            values,
            withClearerView,
        } = formProps;
        const suggestRubricId = values.rubric.specialization || values.rubric.category;

        const { placeholder, rubricsSearchQuery } = this.state;

        return (
            <>
                {this.props.isWorkerOrder && values.phone && (
                    <FormControl
                        theme={FormControlThemes.Normal}
                        label={oldInputsVariant === '1' ? 'Контакты' : undefined}
                        gap="l"
                    >
                        <TextInput
                            name="customerPhone"
                            text={values.phone + (values.customerName ? ` · ${values.customerName}` : '')}
                            logNode={orderFormLogNodes.phone}
                            disabled
                        />
                    </FormControl>
                )}
                <div ref={this.fieldToRefMap.title.ref}>
                    <FormControl
                        theme={FormControlThemes.Normal}
                        label={oldInputsVariant === '1' ? 'Что нужно сделать' : undefined}
                        noGap
                    >
                        {withoutTitleInput ? undefined : (
                            <OrderTitleSuggestField
                                name="title"
                                className={cnTestOrderForm('Title')}
                                placeholder={placeholder?.title || 'Короткое название задачи'}
                                withCapitalizedValue
                                rubricId={enableSuggestRubrics ? suggestRubricId : undefined}
                                onChange={this.onTitleSuggestChange}
                                onRubricChange={this.createOnTitleSuggestRubricChange(formProps)}
                                filterByRubric={Boolean(filterSuggestResults)}
                                focusOnInit={this.focusOnInit && !rubricsSearchQuery}
                                hideValidationError
                            />
                        )}
                    </FormControl>
                </div>
                <div ref={this.fieldToRefMap.description.ref}>
                    <FormControl
                        theme={FormControlThemes.Normal}
                        label={oldInputsVariant === '1' ? 'Описания и пожелания' : undefined}
                        gap={this.textAreaControlGap}
                        className={cnOrderForm('DescriptionFormControl', { withClearerView })}
                    >
                        {this.renderTextArea(formProps)}
                    </FormControl>
                </div>
            </>
        );
    }

    protected renderTextArea(formProps: IOrderFormRenderProps) {
        const { descriptionPlaceholder } = formProps;
        const { placeholder, rubricsSearchQuery } = this.state;

        return (
            <TextAreaAutosizeField
                name="description"
                rows={this.props.descriptionRows}
                className={
                    cnOrderForm('Description', { rows: this.props.descriptionRows }, [cnTestOrderForm('Description')])
                }
                placeholder={
                    placeholder?.description ?
                        placeholder.description :
                        descriptionPlaceholder
                }
                logNode={orderFormLogNodes.description}
                hideValidationMessage
                focusOnInit={this.focusOnInit && Boolean(rubricsSearchQuery)}
                onFocus={this.showDescriptionTooltip}
                onBlur={this.hideDescriptionTooltip}
                onChangeCb={this.onDescriptionSuggestChange(formProps)}
            />
        );
    }

    protected renderSubmitButton(formProps: IOrderFormRenderProps) {
        const { errors, submitButtonText } = formProps;

        return (
            <Button
                className={cnOrderForm('SubmitButton')}
                type="submit"
                onClick={this.createOnSubmitClick(errors, formProps)}
                theme="action"
                logNode={orderFormLogNodes.submit}
                width={this.submitButtonWidth}
            >
                {submitButtonText}
            </Button>
        );
    }

    protected renderUnpublishButton(isUnpublishActionButton = false) {
        const { orderId } = this.props;

        if (!orderId) return;

        return (
            <UnpublishOrderContainer
                orderId={orderId}
                isButton={isUnpublishActionButton}
            />
        );
    }

    protected renderContactsModal(formProps: IOrderFormRenderProps) {
        const {
            handleSubmit,
            isUserAuthorized,
            isUserCallCenterOperator,
            trustedPhones,
            valid,
            form,
            values,
            shouldProvideUserPhone,
            showAddPhoneButton,
        } = formProps;
        const { props } = this;

        if (!isUserAuthorized || isUserCallCenterOperator) {
            return (
                <>
                    <OrderFormContactsModal
                        visible={this.state.isContactsModalVisible}
                        isModalFullScreen={this.isContactsModalFullscreen}
                        isUserAuthorized={isUserAuthorized}
                        isUserCallCenterOperator={isUserCallCenterOperator}
                        retpath={props.retpath}
                        onSubmit={handleSubmit}
                        onPhoneVerificationSubmit={this.onPhoneVerificationSubmit(formProps, true)}
                        onClose={this.createCloseContactsModal(formProps)}
                        phone={values.phone}
                        phoneId={values.phoneId}
                        trustedPhones={trustedPhones}
                        isValid={valid}
                        changeFieldValue={form.change}
                        hideEmailForAnonymous={props.hideEmailForAnonymous}
                    />
                </>
            );
        }

        if (shouldProvideUserPhone || showAddPhoneButton) {
            return (
                <PhoneVerificationModal
                    phone={values.phone}
                    onClose={this.createCloseContactsModal(formProps)}
                    onSubmit={this.onPhoneVerificationSubmit(formProps, this.state.needSubmit)}
                    phoneStepTitle="Смс-уведомления об откликах"
                    description="Добавьте номер телефона, чтобы быстро получать смс-уведомления о&nbsp;новых откликах на&nbsp;заказ. Без спама"
                    visible={this.state.isContactsModalVisible}
                    fullScreen={this.isContactsModalFullscreen}
                />
            );
        }
    }

    protected renderSimilarOccupationOrderModal(formProps: IOrderFormRenderProps) {
        const similarOccupationOrder = this.getSimilarOccupationOrder(formProps);

        return similarOccupationOrder ? (
            <OrderFormSimilarOrderContainer
                visible={this.state.isSimilarOrderVisible}
                orderId={similarOccupationOrder?.id}
                orderTitle={similarOccupationOrder.title}
                onClose={this.closeSimilarOrder}
                onCreateOrderClick={this.createCloseSimilarOccupationOrderModal(similarOccupationOrder, formProps)}
            />
        ) : null;
    }

    protected renderConfirmedOrder() {
        return this.props.withConfirmedOrder && this.props.onCloseConfirmedOrder ? (
            <ConfirmedOrderContainer
                onClose={this.props.onCloseConfirmedOrder}
                onNewOrderClick={this.props.onCloseConfirmedOrder}
            />
        ) : null;
    }

    @boundMethod
    private onTitleSuggestChange(value: string) {
        this.setState({ rubricsSearchQuery: value });
    }

    @boundMethod
    private onDescriptionSuggestChange(formProps: IOrderFormRenderProps) {
        return (value: string) => {
            if (formProps.values.rubric.specialization === Specializations.InternetMarketer) {
                this.setState({ rubricsSearchQuery: value });
            } else if (this.state.rubricsSearchQuery !== formProps.values.title) {
                this.setState({ rubricsSearchQuery: undefined });
            }
        };
    }

    @boundMethod
    protected createOnSubmitClick(errors: AnyObject, formProps: IOrderFormRenderProps) {
        const similarOccupationOrder = this.getSimilarOccupationOrder(formProps);

        if (similarOccupationOrder && similarOccupationOrder.rubrics[0] !== this.state.verifiedSimilarOrderOccupation) {
            return (event?: React.MouseEvent): boolean => {
                event?.preventDefault();
                this.openSimilarOrder();

                return false;
            };
        }

        if (formProps.withHidePhonesFromDescription) {
            const phoneInDescription = this.findUntrustedPhoneInDescription(formProps);

            if (phoneInDescription) {
                return (event?: React.MouseEvent): boolean => {
                    event?.preventDefault();

                    return this.linkPhoneFromDescription(phoneInDescription, formProps);
                };
            }
        }

        if (!isEmpty(errors)) {
            return (event?: React.MouseEvent): boolean => {
                event?.preventDefault();

                const contactsModalFieldNames: string[] = this.getContactsModalFieldNames();
                const firstInvalidNonContactField = Object.keys(errors)
                    .find(fieldName => !contactsModalFieldNames.includes(fieldName));

                if (!firstInvalidNonContactField) {
                    this.showContactsModal(true);
                } else {
                    this.setState({
                        validatingFieldName: firstInvalidNonContactField,
                        validationTooltipOptions: this.fieldToRefMap[firstInvalidNonContactField],
                        isValidationTooltipVisible: true,
                        isPriceTooltipVisible: false,
                    });
                }

                return false;
            };
        }

        return () => true;
    }

    @boundMethod
    private closeValidationTooltip() {
        this.setState({ isValidationTooltipVisible: false });
    }

    @boundMethod
    protected openPriceTooltip() {
        this.setState({ isPriceTooltipVisible: true, isDescriptionTooltipVisible: false });
    }

    @boundMethod
    private closePriceTooltip() {
        this.setState({ isPriceTooltipVisible: false });
    }

    @boundMethod
    private showContactsModal(needSubmit: boolean) {
        this.setState({
            isContactsModalVisible: true,
            needSubmit,
        });
        this.props.onInternalModalToggle?.(true);
    }

    @boundMethod
    protected createCloseContactsModal(formProps: IOrderFormRenderProps) {
        const { onInternalModalToggle } = this.props;
        const { form } = formProps;

        return () => {
            this.setState({ isContactsModalVisible: false });

            onInternalModalToggle?.(false);

            form.change('phone', undefined);
        };
    }

    @boundMethod
    protected createCloseSimilarOccupationOrderModal(order: IOrder, formProps: IOrderFormRenderProps) {
        return () => {
            this.setState({
                verifiedSimilarOrderOccupation: order.rubrics[0],
                isSimilarOrderVisible: false,
            }, () => {
                if (formProps.withHidePhonesFromDescription) {
                    const phoneInDescription = this.findUntrustedPhoneInDescription(formProps);

                    if (phoneInDescription) {
                        this.linkPhoneFromDescription(phoneInDescription, formProps);

                        return;
                    }
                }

                return formProps.form.submit();
            });

            const { onInternalModalToggle } = this.props;

            onInternalModalToggle?.(false);
        };
    }

    private findUntrustedPhoneInDescription(formProps: IOrderFormRenderProps): string | undefined {
        const { phonesList } = formProps;

        const rawPhoneInDescription = findPhonesInStr(formProps.values.description)[0];
        const phoneInDescription = rawPhoneInDescription ?
            addPlusToPhone(replaceEight(cleanPhone(rawPhoneInDescription))) :
            undefined;
        const isPhoneTrusted = phonesList.find(phoneData => phoneData.text === phoneInDescription);

        return isPhoneTrusted ? undefined : phoneInDescription;
    }

    private linkPhoneFromDescription(phone: string, formProps: IOrderFormRenderProps) {
        formProps.form.change('phone', phone);

        this.showContactsModal(true);

        return true;
    }

    @boundMethod
    private openSimilarOrder() {
        this.setState({
            isSimilarOrderVisible: true,
            isValidationTooltipVisible: false,
            isPriceTooltipVisible: false,
            isDescriptionTooltipVisible: false,
        });
        this.props.onInternalModalToggle?.(true);
    }

    @boundMethod
    protected closeSimilarOrder() {
        this.setState({ isSimilarOrderVisible: false });

        const { onInternalModalToggle } = this.props;

        onInternalModalToggle?.(false);
    }

    @boundMethod
    protected onUploadingImagesCountChange(uploadingImagesCount: number) {
        this.setState({ uploadingImagesCount });
    }

    @boundMethod
    rubricsSuggestQuery(rubricId: string | undefined, formProps: IOrderFormRenderProps) {
        const { searchRubrics } = this.props;
        const { rubricsSearchQuery, isLoadingRubrics } = this.state;

        if (isLoadingRubrics) {
            return;
        }

        this.setState({ isLoadingRubrics: true });

        return searchRubrics(rubricsSearchQuery, rubricId)
            .then(result => this.debouncedHandleSuggestResponse(
                result, undefined, Boolean(rubricId), formProps, rubricId,
            ));
    }

    private debouncedHandleSuggestResponse = debounce(this.handleSuggestResponse, REQUEST_DELAY);

    @boundMethod
    private handleSuggestResponse(
        response: ISearchRubricsResponse,
        titleSuggestRubric: string | undefined,
        ignoreRubricChanges: boolean,
        formProps: IOrderFormRenderProps,
        fallbackRubricId?: string,
    ) {
        const {
            descriptionPlaceholder,
            form,
            sendRubricsSuggested,
            specializationsById,
            specializationsBySeoId,
        } = formProps;

        const wasRubricSelectByUser = this.selectedByUser.rubric;

        if (!titleSuggestRubric) { // если последний вызов был из search_rubrics а не садджеста.
            titleSuggestRubric = this.state.titleSuggestRubric || fallbackRubricId;
        }

        if (isEmpty(response) && !titleSuggestRubric) {
            this.setState({ isLoadingRubrics: false });

            if (formProps.values.title) {
                sendRubricsSuggested?.(false);
            }

            return;
        }

        const { rubrics, placeholder, address, orderCreationTooltip } = response;
        const formattedPlaceholder = {
            title: `Например, ${(placeholder?.title || DEFAULT_PLACEHOLDERS.title).toLowerCase()}`,
            description: (placeholder?.description || descriptionPlaceholder),
        };

        let areRubricsChanged: boolean;
        let occupationId: string | undefined;
        let specializationId: string | undefined;

        if (!ignoreRubricChanges && rubrics) {
            occupationId = rubrics.occupationId;
            specializationId = rubrics.specializationId;

            if (rubrics) {
                formProps.sendRubricsSuggested?.(true, rubrics.occupationId, rubrics.specializationId);
            } else {
                formProps.sendRubricsSuggested?.(false);
            }
        }

        if (!specializationId && titleSuggestRubric) {
            const specializationInfo = specializationsBySeoId[titleSuggestRubric] ||
                specializationsById[titleSuggestRubric];

            if (specializationInfo?.parentId) {
                occupationId = specializationInfo.parentId;
                specializationId = specializationInfo.id;
            }
        }

        if (!occupationId) {
            areRubricsChanged = false;
        } else {
            const { category, specialization } = formProps.values.rubric;
            const isSameResponse = category === occupationId && specialization === specializationId;

            areRubricsChanged = !isSameResponse;
        }

        const updatesOnRubricChanges = () => {
            this.updateAddressTypeOnRubricChange(specializationId || occupationId, formProps);
            this.updateAddressesFromRubricsSuggest(address, formProps);
            this.updatePriceOnRubricChange(rubrics, formProps);
        };

        if (!areRubricsChanged || wasRubricSelectByUser) {
            // Если никакие рубрики не сматчились по запросу или сматченные по запросу рубрики совпадают с теми,
            // что уже установлены, то просто подставляем плэйсхолдеры и адреса безо всяких задержек
            this.setState({
                placeholder: formattedPlaceholder,
                orderCreationTooltip,
                isLoadingRubrics: false,
            });

            updatesOnRubricChanges();
        } else {
            if (this.timeout) {
                window.clearTimeout(this.timeout);
                this.timeout = null;
            }

            this.timeout = window.setTimeout(() => {
                this.setState({
                    orderCreationTooltip,
                    isLoadingRubrics: false,
                    placeholder: formattedPlaceholder,
                    titleSuggestRubric: undefined,
                });

                if (occupationId && specializationId) {
                    form.change('rubric', {
                        category: occupationId,
                        specialization: specializationId,
                    });
                }

                updatesOnRubricChanges();
            }, ANSWER_DELAY);
        }
    }

    private updateAddressesFromRubricsSuggest(address: IOrderAddress | undefined, formProps: IOrderFormRenderProps) {
        const { form, values } = formProps;
        const { addressSuggestGeoId } = this.state;

        if (!address?.name || address.geoid === addressSuggestGeoId) {
            return;
        }

        this.setState({
            addressSuggestGeoId: address.geoid,
        });

        if (values.orderAddressType === OrderAddressType.AtCustomer) {
            if (!values.orderAddressTouched) {
                this.setState({ preventDeviceGeoAutoDetection: true });

                form.change('orderAddress', address);
            }

            form.change('orderDistrict', address);
        } else if (values.orderAddressType === OrderAddressType.AtWorker) {
            this.setState({ preventDeviceGeoAutoDetection: true });
            form.change('orderAddress', address);

            !values.orderAddressTouched && form.change('orderDistrict', address);
        }
    }

    @boundMethod
    private createOnTitleSuggestRubricChange(formProps: IOrderFormRenderProps) {
        return (rubricId?: string) => {
            this.setState({ titleSuggestRubric: rubricId });

            const wasRubricSelectByUser = this.selectedByUser.rubric;

            if (!wasRubricSelectByUser) {
                this.debouncedHandleSuggestResponse({}, rubricId, false, formProps);
            }
        };
    }

    private updateAddressTypeOnRubricChange(rubricId: string | undefined, formProps: IOrderFormRenderProps): void {
        const {
            isExistedOrder,
            specializationsMap,
            form,
            values,
        } = formProps;

        if (isExistedOrder) {
            return;
        }

        rubricId && updateAddressTypeOnRubricChange({
            rubricId,
            currentAddressType: values.orderAddressType,
            isAddressTouched: Boolean(this.selectedByUser.orderAddress),
            isAddressTypeTouched: Boolean(this.selectedByUser.orderAddressType),
            specializationsMap,
            changeAddressType: (addressType: OrderAddressType) => form.change('orderAddressType', addressType),
        });
    }

    @boundMethod
    protected createOnRubricChange(formProps: IOrderFormRenderProps) {
        return (rubricId: string) => {
            this.updateAddressTypeOnRubricChange(rubricId, formProps);
            this.handleFieldSelectByUser('rubric');
            this.onRubricsSuggestChange(rubricId, formProps);
        };
    }

    private onRubricsSuggestChange(rubricId: string, formProps: IOrderFormRenderProps) {
        rubricId && this.debouncedRubricsSuggestQuery(rubricId, formProps);
    }

    private updatePriceOnRubricChange(
        rubrics: ISearchRubricsResponse['rubrics'],
        formProps: IOrderFormRenderProps,
    ): void {
        const {
            isExistedOrder,
            withPriceSuggest,
            form,
            sendPriceSuggested,
        } = formProps;

        if (!rubrics || isExistedOrder || !withPriceSuggest || !isPriceKeyExist(withPriceSuggest)) {
            return;
        }

        const { suggestPrice, occupationId, specializationId } = rubrics;

        if (!suggestPrice) {
            sendPriceSuggested?.(false, occupationId, specializationId);

            return;
        }

        form.change('orderPriceMeasure', suggestPrice.measure);
        form.change('orderPrice', suggestPrice[withPriceSuggest]);
        this.openPriceTooltip();

        sendPriceSuggested?.(true, occupationId, specializationId);
    }

    private getContactsModalFieldNames(): Array<keyof IOrderFormValues> {
        const {
            isUserCallCenterOperator,
            isUserAuthorized,
            shouldProvideUserPhone,
            hideEmailForAnonymous,
        } = this.props;

        if (!isUserAuthorized) {
            return hideEmailForAnonymous ?
                ['customerName', 'phone'] :
                ['customerName', 'customerEmail', 'phone'];
        }

        if (isUserCallCenterOperator) {
            return ['customerName', 'phone'];
        }

        return shouldProvideUserPhone ?
            ['customerEmail', 'phone'] :
            ['customerEmail'];
    }

    @boundMethod
    protected createOnFormMounted(formProps: IOrderFormRenderProps) {
        return () => {
            const initialRubric = formProps.initialValues?.rubric;

            this.debouncedRubricsSuggestQuery(initialRubric?.specialization, formProps);
        };
    }

    @boundMethod
    protected onFormStateChange(
        state: FormState<IOrderFormValues>,
        formProps: IOrderFormRenderProps,
    ) {
        const { specializationsById, form } = formProps;
        const { values } = state;
        const { isFormMounted, rubricsSearchQuery } = this.state;
        const isRemoteAllowed = Boolean((specializationsById[values.rubric.specialization] || {}).remotely);

        if (!isFormMounted) {
            return;
        }

        if (!isRemoteAllowed && values.orderAddressType === OrderAddressType.Remote) {
            form.change('orderAddressType', OrderAddressType.AtCustomer);
        }

        if (rubricsSearchQuery !== values.title) {
            this.debouncedRubricsSuggestQuery(undefined, formProps);
        }
    }

    @boundMethod
    protected onPhoneVerificationSubmit(formProps: IOrderFormRenderProps, needSubmit: boolean) {
        const { form } = formProps;

        return (phoneId: string) => {
            form.change('phoneId', phoneId);

            needSubmit && form.submit();
        };
    }

    @boundMethod
    protected onAddPhoneClick() {
        return () => this.showContactsModal(false);
    }

    protected renderAddPhoneBlock(formProps: IOrderFormRenderProps, gapBottom: Gaps) {
        const { showAddPhoneButton, email } = formProps;

        if (!showAddPhoneButton || this.props.isWorkerOrder) return;

        return (
            <OrderFormAddPhone
                email={email}
                gapBottom={gapBottom}
                openPhoneVerificationModal={this.onAddPhoneClick()}
            />
        );
    }

    private getSimilarOccupationOrder(formProps: IOrderFormRenderProps): IOrder | undefined {
        return formProps.sameOccupationRecentOrdersMap ?
            formProps.sameOccupationRecentOrdersMap[formProps.values.rubric.category] :
            undefined;
    }
}

import * as React from 'react';
import { IClassNameProps } from '@bem-react/core';

import { ICategoryPlaceholder, ISearchRubricsResponse } from '../../../api/internal/interfaces/rubrics';
import { ITextSizes } from '../../../features/ui-kit/components/Text/Text';
import { FileModel, IRubric } from '../../../models';
import {
    IOrder,
    IOrderAddress,
    IOrderDateRange,
    IOrderId,
    IOrderNotificationsAgreement,
    OrderAddressType,
    OrderDateType,
    OrderOriginType,
    OrderPriceType,
    OrderStatus,
    OrderTimeOfDay,
} from '../../../models/order';
import { IPaymentType } from '../../../models/payments';
import { PriceMeasure } from '../../../models/price';
import { IWorkerId } from '../../../models/worker';
import { IBaseFormProps, IBaseFormRenderProps } from '../../Form/Form';

export interface IOrderFormRubricValue {
    category: string;
    specialization: string;
}

export interface IOrderFormValues {
    paymentType?: IPaymentType;
    title: string;
    rubric: IOrderFormRubricValue;
    services?: string[];
    description: string;
    docs?: FileModel[];
    phoneId?: string;
    phone?: string;
    showAddPhoneButton?: boolean;
    email?: string | undefined;
    customerName: string;
    withCustomerName?: boolean;
    customerEmail: string | null;

    orderAddress?: IOrderAddress;
    orderAddressType: OrderAddressType;
    orderAddressTouched?: boolean;
    orderDistrict?: IOrderAddress;

    orderDate?: Date;
    orderDateType: OrderDateType;
    orderDateTime?: OrderTimeOfDay;
    orderDateRange?: IOrderDateRange;

    orderPrice?: number;
    orderPriceType: OrderPriceType;
    orderPriceMeasure: PriceMeasure;

    origin: OrderOriginType;

    showCustomerPhone: boolean;
    showPhoneForInvitedWorkers: boolean;

    searchable: boolean;
    suggestToWorkers: IWorkerId[];
    isUserCallCenterOperator?: boolean;
    isUserAuthorized?: boolean;
    isExistedOrder?: boolean;

    passDescriptionValidation?: boolean;
    passEmailValidation?: boolean;
    isWorkerOrder?: boolean;
    shouldProvideUserPhone?: boolean;

    notifications?: IOrderNotificationsAgreement;
}
export type IOrderFormKeys = keyof IOrderFormValues;

export interface IOrderFormOwnProps extends IClassNameProps {
    isLoading?: boolean;
    isExistedOrder?: boolean;

    titleSize?: ITextSizes;
    titleLineSize?: ITextSizes;
    descriptionRows?: number;
    phonesList: Array<{ val: string, text: string }>;
    customerPhone?: string;
    customerName?: string;
    phoneId?: string;
    formTitle: string;
    submitButtonText: string;
    descriptionPlaceholder: string;
    retpath: string;
    rubricsSearchQuery?: string;
    /**
     * Скрывать поле для ввода email в форме оставления контактов при создании заказа незалогиненным пользователем.
     * Нужно для эксперимента `hide_create_order_email_for_anonymous`
     */
    hideEmailForAnonymous?: boolean;
    withSticky?: boolean;
    noTitle?: boolean;
    noTitleField?: boolean;
    workerId?: IWorkerId;
    // при переписывании формы на FinalForm - убрать поле withCustomerNameField
    // сейчас оно прорастает в функцию валидации, для того, чтобы не валидировать поле имени заказчика для незалогинов
    // необходимо научиться правильно считать чиселку невалидных полей формы без этого костыля
    withCustomerNameField?: boolean;

    withConfirmedOrder?: boolean;
    withPaymentType?: boolean;

    openPhoneVerificationModal: () => void;
    onAdd?: (urls: FileModel[]) => void;
    disableSuggestOnTitle?: boolean;
    enableSuggestRubrics?: boolean;
    filterSuggestResults?: boolean;
    withPriceSuggest?: string;
    sendRubricsSuggested?: (isSuggested: boolean, occupationId?: string, specializationId?: string) => void,
    sendPriceSuggested?: (isSuggested: boolean, occupationId?: string, specializationId?: string) => void,

    isMultistep?: boolean;
    onSubmitButtonClick?: (result: SubmitButtonResults) => void;

    specializationsMap?: Record<string, IRubric>
    sameOccupationRecentOrdersMap?: Record<string, IOrder>
    isUserCallCenterOperator: boolean;
    priceMeasures?: PriceMeasure[];

    withStickySubmitButton?: boolean;
    withoutTitleInput?: boolean;
    withDisabledRubricField?: boolean;
    withNewValidationRules?: boolean;
    trustedPhones?: Record<string, string>;
    isUserAuthorized: boolean;
    specializationsById: Record<string, IRubric>;
    specializationsBySeoId: Record<string, IRubric>;
    withOldInputs: boolean;
    oldInputsVariant?: string;
    withOrderCreationTooltip?: boolean;
    orderCreationTooltipText?: string;
    isWorkerOrder?: boolean;
    hideCustomerPhone?: boolean;

    orderId?: IOrderId;
    orderStatus?: OrderStatus;

    onInternalModalToggle?: (toggle: boolean) => void;
    openModal?: (id: string) => void;
    phone?: string;
    showAddPhoneButton?: boolean;
    email?: string | undefined;
    showCustomerPhone?: boolean;
    onSubmitSuccess?: () => void;
    noCategoryField?: boolean;
    hasInvitedWorker?: boolean;
    shouldProvideUserPhone?: boolean;

    fetchLatestOrders?: () => void;

    onCloseConfirmedOrder?: () => void;
    withHidePhonesFromDescription?: boolean;

    isUserPlace?: boolean;
    isHomeFallbackAddress?: boolean;

    focusOnInit?: boolean;

    withNotifications?: boolean;
    withClearerView?: boolean;
    withPromoRefund?: boolean;
    withHypergeoOrders: boolean;
    isSearchPage?: boolean;
    onBack?: () => void;
    searchRubrics: (
        query: string | undefined,
        rubricId: string | undefined,
    ) => Promise<ISearchRubricsResponse>;
}

export const enum SubmitButtonResults {
    Create = 'created',
    Error = 'error',
    NextStep = 'next_step',
}

export interface IOrderFormValidationTooltipOptions {
    text?: string;
    ref?: React.RefObject<HTMLDivElement>,
    offset?: number
}

export interface IOrderFormState {
    isContactsModalVisible: boolean;
    isSimilarOrderVisible: boolean;
    verifiedSimilarOrderOccupation?: string;
    isValidationTooltipVisible: boolean;
    isPriceTooltipVisible: boolean;
    isDescriptionTooltipVisible: boolean;
    isDescriptionTooltipClosed: boolean;
    validatingFieldName?: string;
    validationTooltipOptions?: IOrderFormValidationTooltipOptions;
    fieldToRefMap?: Record<string, React.RefObject<unknown>>;
    uploadingImagesCount: number;
    titleSuggestRubric?: string;
    orderCreationTooltip?: any;
    placeholder?: ICategoryPlaceholder;
    addressSuggestGeoId?: number;
    isLoadingRubrics?: boolean;
    showValidationErrors: boolean;
    preventDeviceGeoAutoDetection?: boolean;
    isFormMounted?: boolean;
    needSubmit: boolean;
    rubricsSearchQuery?: string;
}

export type IOrderFormProps = IBaseFormProps<IOrderFormValues> & IOrderFormOwnProps;
export type IOrderFormRenderProps = IBaseFormRenderProps<IOrderFormProps>;

import { Dispatch } from 'redux';

import { searchRubricsByText } from '../../../../api/internal/rubrics';
import { RootState } from '../../../../store';
import { isExpFlagEnabled } from '../../../../store/experiment/selectors';
import { openModal } from '../../../../store/ui/modalWindow/actions';
import { isUserCallCenterOperator } from '../../../../store/yaUser/selectors';
import { PHONE_VERIFICATION_MODAL_ID } from '../../../PhoneVerificationModal/constants';
import { IOrderFormOwnProps } from '../types';

export type ICommonStateProps = Pick<IOrderFormOwnProps,
    | 'isUserCallCenterOperator'
    | 'withClearerView'
    | 'withPaymentType'
    | 'searchRubrics'>;

export const mapStateToCommonProps = (state: RootState): ICommonStateProps => ({
    isUserCallCenterOperator: isUserCallCenterOperator(state),
    withPaymentType: isExpFlagEnabled(state, 'payments_enable'),
    searchRubrics: searchRubricsByText,
});

export type ICommonDispatchProps = Pick<IOrderFormOwnProps, 'openPhoneVerificationModal'>;

export const mapDispatchToCommonProps = (dispatch: Dispatch): ICommonDispatchProps => ({
    openPhoneVerificationModal: () => dispatch(openModal(PHONE_VERIFICATION_MODAL_ID)),
});

export interface IT {}

import * as Scroll from 'react-scroll';
import { push, replace, RouterState } from 'connected-react-router';
import { Location } from 'history';
import { isEmpty, omit } from 'lodash';
import { Task } from 'redux-saga';
import { call, cancel, delay, fork, put, select, takeEvery, takeLatest } from 'redux-saga/effects';
import { ActionType, getType } from 'typesafe-actions';

import { IHomeRubricsResponse } from '../../api/internal/interfaces/rubrics';
import {
    getWorker as apiGetWorker,
    search as apiSearch,
    searchSecondary as apiSearchSecondary,
} from '../../api/internal/search';
import { ISearchAndWorkers } from '../../data-transformers/searchAndWorkers';
import { IRubric, IWorkerId, IWorkersMap, SearchView } from '../../models';
import { IGeometry } from '../../models/map';
import { IGeoFilter, SearchFilterItemType } from '../../models/searchFilter';
import { pointInSpan } from '../../utils/map/containsPoint';
import { ymReachGoal } from '../../utils/metrika';
import {
    buildSearchUrl,
    changeSearchParams,
    createRegionPath,
    findGeoFilter,
    findRubricsFilter,
    getProfileUrl,
    searchKeyFromPath,
} from '../../utils/search/search';
import { addServiceParams } from '../../utils/url';
import * as directActions from '../direct/actions';
import { addWorkerCounters } from '../direct/actions';
import { getExpFlagNumberValue, isExpFlagEnabled } from '../experiment/selectors';
import { PageName } from '../misc/pageName';
import { getCurrentPage, pageIsCurrent } from '../misc/selectors/page';
import { changeRegion, gpSaveReloadBackend } from '../region/actions';
import * as routerActions from '../router/actions';
import { getCurrentUrl, getLocation, getRouter } from '../router/selectors';
import { fetchHomeRubrics } from '../rubrics/saga';
import { byId, needFetchHomeRubrics } from '../rubrics/selectors';
import * as loaderActions from '../ui/loader/actions';
import { addWorkers } from '../workers/actions';
import { getSearchWorkerIds, getWorkers, getWorkerSearchId, getWorkerSeoname } from '../workers/selectors';
import { getWorkerBySeonameOrId } from '../workers/selectors/getWorkerBySeonameOrId';

import * as searchActions from './actions';
import { clearResponseStatus, replacePromotedWorkers, savePromotedWorkers, SearchClientFromParam } from './actions';
import { getInitSearchParams, ISearchDirectItems, ISearchMapParams, ISearchMisspell, ISearchParams } from './reducer';
import {
    getActiveSearchFilters,
    getGeoFilter,
    getRubricsFilter,
    getSearchMisspell,
    getSearchParams,
    getSearchResultsKey,
    isSearchPage,
    needFetchSearchData,
} from './selectors';

const scroller = Scroll.animateScroll;
const SCROLL_DURATION = 500;
const BEST_WORKERS_DEFAULT_NUMBER = 10;

// Переменная, в которой хранится текущий таск fetchSearchData.
// Необходима для возможности прерывания этой задачи.
let searchTask: Task;

/*
 * Принимает тип и значение параметра поиска. Выставляет нужный url запроса
 *
 * @param {ActionType<typeof searchActions.changeSearchQuery>} action
 */
export const changeSearchQuery = function* (action: ActionType<typeof searchActions.changeSearchQuery>) {
    const { type, value, rubricId, regionData, from } = action.payload;
    const currentParams: ISearchParams = yield select(getSearchParams);
    const initialParams = getInitSearchParams(currentParams);
    let searchParams: ISearchParams;
    let rubric: IRubric | undefined;

    if (rubricId) {
        rubric = yield select(byId, rubricId);
    }

    searchParams = changeSearchParams({
        type,
        value,
        rubric,
        regionData,
        from,
    }, currentParams, initialParams);

    // Если стреляет изменение страницы или перезапрос из поисковой строки, подскролливаем страницу наверх
    if (type === 'page' || type === 'query') {
        scroller.scrollTo(0, { duration: SCROLL_DURATION });
    }

    const useRubrics = Boolean(rubric);

    yield put(searchActions.search(
        buildSearchUrl(searchParams, { useRubrics }),
        {
            setUrl: true,
            withDelay: false,
            setResponseUrl: false,
            fromSuggest: from === SearchClientFromParam.Suggest,
        },
    ));
};

export const savePromotedWorkersForMap = function* (result: ISearchAndWorkers) {
    const isEnabledPromotedWorkersWithoutSave = yield select(isExpFlagEnabled, 'enable_promoted_workers_on_map');
    const isEnabledPromotedWorkersWithSave = yield select(isExpFlagEnabled, 'enable_promoted_workers_on_map_with_save');

    if (!(isEnabledPromotedWorkersWithoutSave || isEnabledPromotedWorkersWithSave)) return;

    const promotedWorkers = result.search.workerIds.reduce<string[]>((acc, worker) => {
        if (result.workers.items[worker].isAdvertised && !acc.includes(worker)) {
            acc.push(worker);
        }

        return acc;
    }, []);

    if (isEnabledPromotedWorkersWithoutSave) {
        yield put(replacePromotedWorkers(promotedWorkers));

        return;
    }

    yield put(savePromotedWorkers(promotedWorkers));
};

export const getSearchResult = function* (searchKey: string) {
    let result: ISearchAndWorkers = yield call(apiSearch, searchKey);
    const softnessValue = yield select(getExpFlagNumberValue, 'search_softness');

    if (!isEmpty(result) && result.search.params.pagination.totalItems < 6 && softnessValue) {
        result = yield call(apiSearch, searchKey, { wizextra: `ydosoftness=${softnessValue}` });
    }

    return result;
};

export const openSearchUrl = function* (url: string, handler: typeof replace | typeof push, withState?: boolean) {
    const router: RouterState = yield select(getRouter);

    yield put(handler(url, withState ? router.location.state : undefined));
};

function* filterSearchResultBySpan(searchResult: ISearchAndWorkers, searchMapParams: ISearchMapParams) {
    const prevWorkerIds: IWorkerId[] = yield select(getSearchWorkerIds);
    const workers: IWorkersMap = {
        ...(yield select(getWorkers)),
        ...searchResult.workers.items,
    };

    return [...new Set(prevWorkerIds.concat(searchResult.search.workerIds))].filter(id => {
        return workers[id]?.geometry?.some((geometry: IGeometry) => {
            if (geometry.type !== 'Point' || !geometry.coordinates) {
                return false;
            }

            return pointInSpan(geometry.coordinates, searchMapParams.center, searchMapParams.span);
        });
    });
}

export const fetchSearchData = function* (action: ActionType<typeof searchActions.search>,
) {
    const {
        payload: searchKey,
        meta: {
            setUrl,
            withDelay,
            isShowMore,
            fromSuggest,
            withReplace,
            setResponseUrl,
            fromRubricFilter,
            preserveSearchResults,
        },
    } = action;

    yield put(searchActions.searchRequest(searchKey, { isShowMore }));

    try {
        if (withDelay) {
            yield delay(800);
        }

        const location: ReturnType<typeof getLocation> = yield select(getLocation);

        if (setUrl) {
            yield call(openSearchUrl, addServiceParams(location.search, searchKey), push, true);
        }

        const result: ISearchAndWorkers = yield call(getSearchResult, searchKey);
        const searchUrlParams = {
            ...result.search.params,
            from: fromRubricFilter ? SearchClientFromParam.Filter : undefined,
        };

        // Рассчитываем, что валидный url можем получить только на основе фильтров из ответа поиска
        if (setResponseUrl) {
            const responseUrl = addServiceParams(
                location.search, buildSearchUrl(searchUrlParams),
            );
            const currentUrl = yield select(getCurrentUrl);

            // не нужно пушить новый урл, если есть опечаточник
            if (responseUrl !== currentUrl && !result.search.misspell) {
                yield call(
                    openSearchUrl,
                    responseUrl,
                    withReplace ? replace : push,
                    true,
                );
            }
        }

        if (isEmpty(result) || isEmpty(result.search.workerIds)) {
            ymReachGoal('EMPTY_SERP');
        }

        yield call(savePromotedWorkersForMap, result);

        const { filterGeo } = action.meta;

        if (filterGeo) {
            result.search.workerIds = yield call(filterSearchResultBySpan, result, filterGeo);
        }

        yield put(searchActions.searchSuccess(
            result,
            { isShowMore, preserveSearchResults, fromSuggest },
        ));

        yield put(directActions.addWorkerCounters(result.direct));
    } catch (e) {
        yield put(searchActions.searchError(e));
    }
};

// Метод реагирует на смену урла поиска в браузере
export const fetchIfNeededSearchData = function* (action: ActionType<typeof searchActions.searchIfNeeded>) {
    const searchKey = searchKeyFromPath(action.payload);

    // Проверяем нужно ли фетчить данные для этого запроса
    // Они могу уже лежать в сторе или запрашиваться в данный момент
    const isNeedFetch: boolean = yield select(needFetchSearchData, searchKey);

    if (!isNeedFetch) {
        return;
    }

    searchTask = yield fork(fetchSearchData, searchActions.search(searchKey, { withDelay: false }));
};

export const fetchIfNeededProfileData = function* (action: ActionType<typeof searchActions.fetchProfileIfNeeded>) {
    const searchKey = action.payload;

    const isNeedFetch: boolean = yield select(needFetchSearchData, searchKey);

    if (!isNeedFetch) {
        return;
    }

    yield put(searchActions.search(searchKey, { withDelay: false, preserveSearchResults: true }));
};

export const fetchProfileDataOnLocationChange = function* () {
    const isPublicProfilePage = yield select(pageIsCurrent, PageName.PublicProfile);

    if (isPublicProfilePage) {
        const worerInitialSeoname = yield select(getWorkerSeoname);
        const worerInitialId = yield select(getWorkerSearchId);
        const workerInitial = yield select(getWorkerBySeonameOrId, worerInitialSeoname, worerInitialId);

        if (!workerInitial) {
            const searchKey = getProfileUrl(worerInitialId, worerInitialSeoname);

            yield fork(fetchSearchData, searchActions.search(
                searchKey, {
                    withDelay: false,
                    preserveSearchResults: true,
                }));
        }
    }
};

export const fetchSearchDataWithoutMisspell = function* () {
    const currentParams: ISearchParams = yield select(getSearchParams);
    const misspell: ISearchMisspell = yield select(getSearchMisspell);

    currentParams.query = misspell.sourceText;
    currentParams.withoutMisspell = true;

    const searchUrl = buildSearchUrl(currentParams);

    yield put(searchActions.search(searchUrl, {
        withDelay: false,
        setResponseUrl: true,
    }));
};

function* getRegionalPageUrl(regionId: number, latinName: string) {
    const location: Location = yield select(getLocation);

    return addServiceParams(
        location.search,
        latinName ? `/${createRegionPath(regionId, latinName)}` : '/',
    );
}

export const changeSearchRegion = function* (action: ActionType<typeof searchActions.changeSearchRegion>) {
    const region = action.payload.region;
    const replacePageUrlToRegional = action.payload.replacePageUrlToRegional;

    const currentParams: ISearchParams = yield select(getSearchParams);
    const geoFilter: IGeoFilter | undefined = yield select(getGeoFilter);
    const isSearch = yield select(isSearchPage);

    const newGeoFilter: IGeoFilter = {
        ...(geoFilter || {
            id: 'geo-filter',
            type: SearchFilterItemType.Geo,
            paramName: [],
        }),
        geoid: region.id,
        geoname: region.name,
        geonameExact: region.geonameExact,
        geonameLatin: region.latinName,
        uri: region.uri || '',
        geonameL10n: region.l10n,
        geotype: region.geotype,
        ydoregion: region.ydoregion,
    };

    yield put(searchActions.setParams({
        ...currentParams,
        from: action.payload.from,
        filters: [
            ...currentParams.filters.filter(f => f.type !== 'geo'),
            newGeoFilter,
        ],
    }));

    if (isSearch) {
        const filters = yield select(getActiveSearchFilters);

        yield put(searchActions.setFilters({
            filters,
            noFetchSearchData: action.payload.noFetchSearchData,
            notSetGeoParamFromCurrentParams: true,
        }));

        return;
    }

    if (replacePageUrlToRegional && (!geoFilter || region.id !== geoFilter.geoid)) {
        const newUrl = yield call(getRegionalPageUrl, region.id, region.latinName);

        yield call(openSearchUrl, newUrl, replace);
    }

    if (action.payload.needScrollToTop) {
        scroller.scrollTo(0, { duration: SCROLL_DURATION });
    }

    const isNeedFetch: boolean = yield select(needFetchHomeRubrics, region);

    if (!isNeedFetch) {
        return;
    }

    const homeRubricsResponse: IHomeRubricsResponse | undefined = yield call(fetchHomeRubrics, region);

    // Если английское название не пришло с регионом, берём его из ответа на запрос рубрик
    const currentParams2: ISearchParams = yield select(getSearchParams);
    const geoFilter2: IGeoFilter | undefined = yield select(getGeoFilter);

    if (!region.latinName && homeRubricsResponse && geoFilter2 && geoFilter2.geoid === region.id) {
        yield put(searchActions.setParams({
            ...currentParams2,
            filters: [
                ...currentParams2.filters.filter(f => f.type !== 'geo'),
                {
                    ...geoFilter2,
                    geonameLatin: homeRubricsResponse.geoLatinName,
                },
            ],
        }));

        if (replacePageUrlToRegional && region.id === geoFilter2.geoid) {
            const newUrl = yield call(getRegionalPageUrl, region.id, homeRubricsResponse.geoLatinName);

            yield call(openSearchUrl, newUrl, replace);
        }
    }
};

export const setFilters = function* (action: ActionType<typeof searchActions.setFilters>) {
    if (searchTask) {
        yield cancel(searchTask);
    }

    const rubricsFilter = findRubricsFilter(action.payload.filters);
    const currentRubricsFilter = yield select(getRubricsFilter);

    const geoFilter = findGeoFilter(action.payload.filters);
    const currentGeoFilter = yield select(getGeoFilter);

    if (geoFilter && geoFilter.geoid !== currentGeoFilter?.geoid) {
        yield put(changeRegion({
            region: {
                id: geoFilter.geoid,
                name: geoFilter.geoname,
                geonameExact: geoFilter.geonameExact,
                latinName: '',
                uri: geoFilter.uri,
                geotype: geoFilter.geotype,
                ydoregion: geoFilter.ydoregion,
            },
        }));
    }

    const currentSearchParams: ISearchParams = yield select(getSearchParams);

    const searchUrl = buildSearchUrl({
        ...currentSearchParams,
        // перезаписываем значения фильтров, которые прилетают из экшена, остальные фильтры не трогаем
        filters: currentSearchParams.filters.map(
            currentFilterItem => action.payload.filters.
            find(actionFilterItem => actionFilterItem.id === currentFilterItem.id) || currentFilterItem,
        ),
        pagination: {
            ...currentSearchParams.pagination,
            p: 0,
        },
    }, { useRubrics: true, notSetGeoParamFromCurrentParams: action.payload.notSetGeoParamFromCurrentParams });

    if (action.payload.noFetchSearchData) {
        const location = yield select(getLocation);

        yield put(replace(addServiceParams(location.search, searchUrl)));
        yield put(searchActions.setKey(searchUrl));
        yield put(clearResponseStatus());
    } else {
        searchTask = yield fork(fetchSearchData, searchActions.search(searchUrl, {
            withDelay: true,
            setResponseUrl: true,
            fromRubricFilter: currentRubricsFilter.value !== rubricsFilter?.value,
        }));
    }
};

export const setMapParams = function* (action: ActionType<typeof searchActions.setMapParams>) {
    if (searchTask) {
        yield cancel(searchTask);
    }

    const location = yield select(getLocation);
    const currentSearchParams: ISearchParams = yield select(getSearchParams);
    const searchUrl = addServiceParams(location.search, buildSearchUrl({
        ...currentSearchParams,
        view: SearchView.Map,
        map: action.payload,
        pagination: {
            ...currentSearchParams.pagination,
            p: 0,
        },
    }));

    searchTask = yield fork(fetchSearchData, searchActions.search(searchUrl, {
        withDelay: false,
        setResponseUrl: true,
        withReplace: true,
        filterGeo: action.payload,
    }));
};

const showMore = function* () {
    const currentSearchParams: ISearchParams = yield select(getSearchParams);
    const { pagination } = currentSearchParams;

    const searchUrl = buildSearchUrl({
        ...currentSearchParams,
        pagination: {
            ...pagination,
            p: pagination.p + 1,
        },
    });

    if (currentSearchParams.view === SearchView.Map) {
        yield put(searchActions.search(searchUrl, {
            withDelay: false,
            isShowMore: true,
            setUrl: true,
            withReplace: true,
        }));
    } else {
        yield put(searchActions.search(searchUrl, {
            withDelay: false,
            isShowMore: true,
        }));
    }
};

export const switchSerpViewMode = function* ({ payload }: ActionType<typeof searchActions.switchViewMode>) {
    const currentPage: ReturnType<typeof getCurrentPage> = yield select(getCurrentPage);

    if (currentPage !== PageName.Search && currentPage !== PageName.SearchMap) return;

    const { view, withHistoryReplace } = payload;

    if (searchTask) {
        yield cancel(searchTask);
    }

    if (view !== SearchView.Map) {
        yield put(clearResponseStatus());
    }

    const location = yield select(getLocation);
    const currentParams: ISearchParams = yield select(getSearchParams);
    const newParams = { ...currentParams };

    yield put(searchActions.setParams(newParams));

    yield call(openSearchUrl, addServiceParams(
        location.search,
        buildSearchUrl(newParams),
    ), withHistoryReplace ? replace : push, true);
};

export const addWorkersIfNeeded = function* (data: ISearchAndWorkers) {
    const workers = yield select(getWorkers);

    const newWorkers = omit(data.workers.items, Object.keys(workers));

    if (!isEmpty(newWorkers)) {
        yield put(addWorkers(newWorkers));
    }
};

export const getWorkerIfNeeded = function* (action: ActionType<typeof searchActions.getWorkerIfNeeded>) {
    const { payload } = action;
    const workers = yield select(getWorkers);

    if (workers[payload]) return;

    try {
        const result: ISearchAndWorkers = yield call(apiGetWorker, payload);

        yield put(addWorkers(result.workers.items));
    } catch (_e) {
        return {};
    }
};

export const getRecommendedWorkers = function* (action: ActionType<typeof searchActions.getRecommendedWorkers>) {
    const { payload, meta: { afterWorkerId, numdoc } } = action;

    yield put(loaderActions.show(`get-recommended-workers-for-${afterWorkerId}`));

    // https://st.yandex-team.ru/YDO-7311
    // Ищем ошибку, когда запрос на бекенд уезжает без текста и категории одновременно
    if (payload.includes('category?') && (payload.includes('text=&') || payload.endsWith('text='))) {
        throw new Error('getRecommendedWorkers без категории и текста запроса');
    }

    try {
        const result: ISearchAndWorkers = yield call(apiSearchSecondary, payload, {
            after_worker_id: afterWorkerId,
            numdoc,
        });
        const directData: ISearchDirectItems = [
            ...(Object.values(result.search.direct || {})),
            ...(Object.values(result.search.garantDirect || {})),
        ].reduce((acc, data, index) => ({
            ...acc,
            [index]: data,
        }), {});

        yield put(addWorkerCounters(result.direct));
        yield call(addWorkersIfNeeded, result);

        yield put(searchActions.getRecommendedWorkersSuccess({
            itemsIds: result.search.workerIds,
            totalItems: result.search.params.pagination.totalItems,
            direct: directData,
        }, afterWorkerId));
    } catch (_e) {
    } finally {
        yield put(loaderActions.hide(`get-recommended-workers-for-${afterWorkerId}`));
    }
};

export const getBestWorkers = function* (action: ActionType<typeof searchActions.getBestWorkers>) {
    const { payload, meta } = action;

    yield put(loaderActions.show('get-best-workers'));

    try {
        const result: ISearchAndWorkers =
            yield call(apiSearchSecondary, payload, { numdoc: meta?.num || BEST_WORKERS_DEFAULT_NUMBER });

        yield put(addWorkerCounters(result.direct));
        yield put(addWorkers(result.workers.items));
        yield put(searchActions.getBestWorkersSuccess({
            itemsIds: result.search.workerIds,
            totalItems: result.search.params.pagination.totalItems,
        }));
    } catch (_e) {
    } finally {
        yield put(loaderActions.hide('get-best-workers'));
    }
};

export const cancelSearchTask = function* () {
    const currentPage: ReturnType<typeof getCurrentPage> = yield select(getCurrentPage);

    if (currentPage !== PageName.Search && currentPage !== PageName.SearchMap && searchTask) {
        yield cancel(searchTask);
    }
};

export const fetchSearchAfterGpSave = function* () {
    if (searchTask) {
        yield cancel(searchTask);
    }

    const currentParams: ISearchParams = yield select(getSearchParams);
    const searchUrl = buildSearchUrl({ ...currentParams });

    searchTask = yield fork(fetchSearchData, searchActions.search(searchUrl, {
        withDelay: false,
        setResponseUrl: true,
    }));
};

export const fetchProfileAfterGpSave = function* () {
    const searchKey = yield select(getSearchResultsKey);

    yield put(searchActions.search(searchKey, { withDelay: false, preserveSearchResults: true }));
};

// TODO: рефактор в https://st.yandex-team.ru/YDO-10658
export const fetchAfterGpSave = function* () {
    const isGeoSerpRequest = yield select(isExpFlagEnabled, 'hypergeo_enable_geo_serp');
    const isGeoSerpTooltipRequest = yield select(isExpFlagEnabled, 'hypergeo_serp_tooltip');
    const isGeoProfileRequest = yield select(isExpFlagEnabled, 'hypergeo_enable_geo_profile');

    if (!isGeoSerpRequest && !isGeoProfileRequest && !isGeoSerpTooltipRequest) {
        return;
    }

    const currentPage: ReturnType<typeof getCurrentPage> = yield select(getCurrentPage);

    if ((isGeoSerpRequest || isGeoSerpTooltipRequest) && currentPage === PageName.Search) {
        yield call(fetchSearchAfterGpSave);
    }

    if (isGeoProfileRequest && currentPage === PageName.PublicProfile) {
        yield call(fetchProfileAfterGpSave);
    }
};

export const searchSaga = function* () {
    yield takeEvery(getType(searchActions.search), fetchSearchData);
    yield takeEvery(getType(searchActions.changeSearchQuery), changeSearchQuery);
    yield takeEvery(getType(searchActions.searchIfNeeded), fetchIfNeededSearchData);
    yield takeEvery(getType(searchActions.fetchProfileIfNeeded), fetchIfNeededProfileData);
    yield takeEvery(getType(searchActions.fetchWithoutMisspell), fetchSearchDataWithoutMisspell);
    yield takeEvery(getType(searchActions.changeSearchRegion), changeSearchRegion);
    yield takeEvery(getType(searchActions.setFilters), setFilters);
    yield takeLatest(getType(searchActions.setMapParams), setMapParams);
    yield takeEvery(getType(searchActions.showMore), showMore);
    yield takeEvery(getType(searchActions.switchViewMode), switchSerpViewMode);
    yield takeLatest(getType(searchActions.getWorkerIfNeeded), getWorkerIfNeeded);
    yield takeEvery(getType(searchActions.getRecommendedWorkers), getRecommendedWorkers);
    yield takeLatest(getType(searchActions.getBestWorkers), getBestWorkers);
    yield takeEvery(getType(searchActions.cancelSearchTask), cancelSearchTask);
    yield takeLatest(getType(gpSaveReloadBackend), fetchAfterGpSave);
    yield takeLatest(getType(routerActions.locationChange), fetchProfileDataOnLocationChange);
};


