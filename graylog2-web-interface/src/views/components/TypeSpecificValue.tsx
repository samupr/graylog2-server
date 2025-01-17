/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import isString from 'lodash/isString';
import trim from 'lodash/trim';
import trunc from 'lodash/truncate';

import Timestamp from 'components/common/Timestamp';
import FieldType from 'views/logic/fieldtypes/FieldType';
import InputField from 'views/components/fieldtypes/InputField';
import NodeField from 'views/components/fieldtypes/NodeField';
import StreamsField from 'views/components/fieldtypes/StreamsField';

import EmptyValue from './EmptyValue';
import CustomPropTypes from './CustomPropTypes';
import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';
import DecoratorValue from './DecoratorValue';

const _formatValue = (field, value, truncate, render, type) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component = render;

  return trim(stringified) === ''
    ? <EmptyValue />
    : <Component field={field} value={(truncate ? trunc(stringified) : stringified)} type={type} />;
};

type Props = {
  field: string,
  value?: any,
  type: FieldType,
  truncate?: boolean,
  render?: React.ComponentType<ValueRendererProps>,
};

const defaultComponent = ({ value }: ValueRendererProps) => value;

const TypeSpecificValue = ({ field, value, render = defaultComponent, type = FieldType.Unknown, truncate = false }: Props) => {
  const Component = render;

  if (value === undefined) {
    return null;
  }

  if (type.isDecorated()) {
    return <DecoratorValue value={value} field={field} render={render} type={type} truncate={truncate} />;
  }

  switch (type.type) {
    case 'date': return <Timestamp dateTime={value} render={render} field={field} format="complete" />;
    case 'boolean': return <Component value={String(value)} field={field} />;
    case 'input': return <InputField value={String(value)} />;
    case 'node': return <NodeField value={String(value)} />;
    case 'streams': return <StreamsField value={value} />;
    default: return _formatValue(field, value, truncate, render, type);
  }
};

TypeSpecificValue.propTypes = {
  truncate: PropTypes.bool,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any,
};

TypeSpecificValue.defaultProps = {
  truncate: false,
  render: defaultComponent,
  type: undefined,
  value: undefined,
};

export default TypeSpecificValue;
